package com.is2rang.tetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "TetrioMobile";
    private static final int GRID_SIZE_DP = 10;
    private static final String PREFS_NAME = "TetroidCustomPadPrefs";
    
    private String launchRoomCode = "";
    private boolean isPadVisible = true;
    private boolean isEditMode = false;
    private VirtualButton selectedButton = null;
    private float displayDensity;
    private GamePadOverlay combinedPad;

    private LinearLayout sizeBar;
    private TextView tvScale;
    private Button btnEditToggle;
    private Button btnVisibilityToggle;
    private Button btnEsc;
    private Button btnR;

    private boolean hasDispatchedGesture = false;
    private WebMessagePort nativePort;

    // [최적화] 고속 터치 입력을 위한 JSON 스트링 사전 캐시 배열 (버튼 16개 * 상태 2개 = 32개 상주)
    private final String[][] gpMessageCache = new String[16][2];

    private class VirtualButton {
        final String saveKey;
        final String label;
        final RectF bounds = new RectF();
        final Rect hitRect = new Rect();
        final int[] gamepadButtonIndices;

        boolean isCurrentPressed = false;
        int activePointerCount = 0;
        boolean selected = false;

        final int baseWidthDp;
        final int baseHeightDp;
        float scaleFactor = 1.0f;

        VirtualButton(String saveKey, String label, int[] gamepadButtonIndices, int baseWidthDp, int baseHeightDp) {
            this.saveKey = saveKey;
            this.label = label;
            this.gamepadButtonIndices = gamepadButtonIndices;
            this.baseWidthDp = baseWidthDp;
            this.baseHeightDp = baseHeightDp;
        }

        float getWidthPx() {
            float grid = dpToPx(GRID_SIZE_DP);
            return Math.round((dpToPx(baseWidthDp) * scaleFactor) / grid) * grid;
        }
        
        float getHeightPx() {
            float grid = dpToPx(GRID_SIZE_DP);
            return Math.round((dpToPx(baseHeightDp) * scaleFactor) / grid) * grid;
        }

        void setBounds(float left, float top) {
            float w = getWidthPx();
            float h = getHeightPx();
            bounds.set(left, top, left + w, top + h);
            rebuildHitRect();
        }

        void rebuildHitRect() {
            hitRect.set(
                    Math.round(bounds.left),
                    Math.round(bounds.top),
                    Math.round(bounds.right),
                    Math.round(bounds.bottom)
            );
        }
    }

    private class GamePadOverlay extends FrameLayout {
        private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint selectedBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final List<VirtualButton> buttons = new ArrayList<>();
        private final SparseArray<VirtualButton> pointerButtonMap = new SparseArray<>();
        private boolean layoutInitialized = false;

        private float dragStartX;
        private float dragStartY;
        private float originLeft;
        private float originTop;

        GamePadOverlay(Context context) {
            super(context);
            setWillNotDraw(false);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);

            buttonPaint.setColor(Color.parseColor("#66000000"));
            selectedBorderPaint.setStyle(Paint.Style.STROKE);
            selectedBorderPaint.setStrokeWidth(dpToPx(3));
            selectedBorderPaint.setColor(Color.WHITE);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(dpToPx(16));
        }

        void addVirtualButton(VirtualButton button) {
            buttons.add(button);
        }

        List<VirtualButton> getButtons() {
            return buttons;
        }

        void setPadVisible(boolean visible) {
            if (!visible) {
                clearAllPressedStates();
            }
            invalidate();
        }

        void initializeButtonLayouts() {
            if (layoutInitialized) return;

            if (getWidth() == 0 || getHeight() == 0) {
                post(this::initializeButtonLayouts);
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            for (VirtualButton btn : buttons) {
                applyStoredOrDefaultLayout(btn, prefs);
            }

            layoutInitialized = true;
            invalidate();
        }

        private void applyStoredOrDefaultLayout(VirtualButton btn, SharedPreferences prefs) {
            String key = btn.saveKey;
            if (prefs.contains(key + "_left") && prefs.contains(key + "_top")) {
                float left = prefs.getFloat(key + "_left", 0f);
                float top = prefs.getFloat(key + "_top", 0f);
                float scale = prefs.getFloat(key + "_scale", 1.0f);

                btn.scaleFactor = clamp(scale, 0.5f, 2.0f);
                btn.setBounds(left, top);
                return;
            }
            applyDefaultLayout(btn);
        }

        private void applyDefaultLayout(VirtualButton btn) {
            int margin = dpToPx(20);
            int gap = dpToPx(10);
            float screenW = getWidth();
            float screenH = getHeight();

            float sizeW = btn.getWidthPx();
            float sizeH = btn.getHeightPx();

            float leftX = margin;
            float midX = leftX + sizeW + gap;
            float rightX = midX + sizeW + gap;

            float bottomY = screenH - margin - sizeH;
            float upperY = bottomY - gap - sizeH;

            float rightGroupRightX = screenW - margin - sizeW;
            float rightGroupMidX = rightGroupRightX - gap - sizeW;
            float rightGroupLeftX = rightGroupMidX - gap - sizeW;

            switch (btn.saveKey) {
                case "left": btn.setBounds(leftX, bottomY); break;
                case "soft_drop": btn.setBounds(midX, bottomY); break;
                case "right": btn.setBounds(rightX, bottomY); break;
                case "l_soft": btn.setBounds(leftX, upperY); break;
                case "r_soft": btn.setBounds(rightX, upperY); break;
                case "hard_drop": btn.setBounds(rightGroupRightX, bottomY); break;
                case "rotate_cw": btn.setBounds(rightGroupMidX, bottomY); break;
                case "rotate_ccw": btn.setBounds(rightGroupLeftX, bottomY); break;
                case "rotate_180": btn.setBounds(rightGroupMidX, upperY); break;
                case "hold": btn.setBounds(rightGroupLeftX, upperY); break;
                default: btn.setBounds(leftX, bottomY); break;
            }
        }

        void saveLayoutsToPrefs() {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            for (VirtualButton btn : buttons) {
                editor.putFloat(btn.saveKey + "_left", btn.bounds.left);
                editor.putFloat(btn.saveKey + "_top", btn.bounds.top);
                editor.putFloat(btn.saveKey + "_scale", btn.scaleFactor);
            }
            editor.apply();
        }

        // [최적화] 매 탭마다 발생하던 HashSet 동적 객체 생성을 완전히 제거함
        void clearAllPressedStates() {
            int size = buttons.size();
            for (int i = 0; i < size; i++) {
                VirtualButton btn = buttons.get(i);
                if (btn.isCurrentPressed) {
                    btn.isCurrentPressed = false;
                    btn.activePointerCount = 0;
                    for (int index : btn.gamepadButtonIndices) {
                        sendGamepadStateToJs(index, false);
                    }
                }
            }
            pointerButtonMap.clear();
            invalidate();
        }

        private VirtualButton findButtonAt(float x, float y) {
            int size = buttons.size();
            for (int i = 0; i < size; i++) {
                VirtualButton btn = buttons.get(i);
                if (btn.hitRect.contains((int) x, (int) y)) {
                    return btn;
                }
            }
            return null;
        }

        private boolean handleEditTouch(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    VirtualButton btn = findButtonAt(event.getX(), event.getY());
                    if (btn == null) return true;

                    selectButton(btn);
                    dragStartX = event.getX();
                    dragStartY = event.getY();
                    originLeft = btn.bounds.left;
                    originTop = btn.bounds.top;
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (selectedButton == null) return true;

                    float dx = event.getX() - dragStartX;
                    float dy = event.getY() - dragStartY;
                    float newLeft = originLeft + dx;
                    float newTop = originTop + dy;

                    float gridPx = dpToPx(GRID_SIZE_DP);
                    newLeft = Math.round(newLeft / gridPx) * gridPx;
                    newTop = Math.round(newTop / gridPx) * gridPx;

                    float maxLeft = Math.max(0, getWidth() - selectedButton.getWidthPx());
                    float maxTop = Math.max(0, getHeight() - selectedButton.getHeightPx());

                    selectedButton.setBounds(clamp(newLeft, 0f, maxLeft), clamp(newTop, 0f, maxTop));
                    invalidate();
                    return true;
                }
            }
            return true;
        }

        private boolean handleGameTouch(MotionEvent event) {
            WebView webView = getCurrentWebView();
            if (webView == null) return false;

            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int pointerId = event.getPointerId(actionIndex);
                    VirtualButton btn = findButtonAt(event.getX(actionIndex), event.getY(actionIndex));
                    pointerButtonMap.put(pointerId, btn);
                    pressButton(webView, btn);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    int pointerCount = event.getPointerCount();
                    for (int i = 0; i < pointerCount; i++) {
                        int pointerId = event.getPointerId(i);
                        VirtualButton oldBtn = pointerButtonMap.get(pointerId);
                        VirtualButton newBtn = findButtonAt(event.getX(i), event.getY(i));

                        if (oldBtn != newBtn) {
                            releaseButton(webView, oldBtn);
                            pressButton(webView, newBtn);
                            pointerButtonMap.put(pointerId, newBtn);
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP: {
                    int pointerId = event.getPointerId(actionIndex);
                    VirtualButton btn = pointerButtonMap.get(pointerId);
                    releaseButton(webView, btn);
                    pointerButtonMap.remove(pointerId);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    clearAllPressedStates();
                    return true;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!isPadVisible) return;

            int size = buttons.size();
            for (int i = 0; i < size; i++) {
                VirtualButton btn = buttons.get(i);
                canvas.drawRect(btn.bounds, buttonPaint);

                if (isEditMode && btn.selected) {
                    canvas.drawRect(btn.bounds, selectedBorderPaint);
                }

                float cx = btn.bounds.centerX();
                float cy = btn.bounds.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f);
                canvas.drawText(btn.label, cx, cy, textPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isPadVisible) return false;
            return isEditMode ? handleEditTouch(event) : handleGameTouch(event);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        launchRoomCode = getIntent().getStringExtra("roomCode");
        displayDensity = getResources().getDisplayMetrics().density;

        // [최적화] 커스텀 고속 데이터 통신용 JSON 인덱스 미리 베이킹(Baking) 처리
        for (int i = 0; i < 16; i++) {
            gpMessageCache[i][0] = "{\"btnIndex\":" + i + ",\"isPressed\":false}"; // Pressed false 상태 캐싱
            gpMessageCache[i][1] = "{\"btnIndex\":" + i + ",\"isPressed\":true}";  // Pressed true 상태 캐싱
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().setFitsSystemWindows(false);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "전체화면 몰입 인프라 예외 생략");
        }

        // [최적화] 람다식 표기 전환
        getWindow().getDecorView().post(() -> {
            try {
                setupAdvancedModularSystem();
            } catch (Exception e) {
                Log.e(TAG, "시스템 빌드 실패", e);
            }
        });
    }

    private void setupAdvancedModularSystem() {
        FrameLayout rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        if (getBridge() != null && getBridge().getWebView() != null) {
            optimizeWebViewPerformance(getBridge().getWebView());
        }

        combinedPad = new GamePadOverlay(this);
        combinedPad.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        combinedPad.setBackgroundColor(Color.TRANSPARENT);

        createVirtualButtons();
        createUtilityControls(combinedPad);
        rootView.addView(combinedPad);

        // [최적화] 람다식 표기 전환
        combinedPad.post(() -> combinedPad.initializeButtonLayouts());
    }

    private void createVirtualButtons() {
        if (combinedPad == null) return;
        combinedPad.addVirtualButton(new VirtualButton("left", "←", new int[]{14}, 100, 100));
        combinedPad.addVirtualButton(new VirtualButton("soft_drop", "↓", new int[]{13}, 100, 100));
        combinedPad.addVirtualButton(new VirtualButton("right", "→", new int[]{15}, 100, 100));
        combinedPad.addVirtualButton(new VirtualButton("l_soft", "↙", new int[]{14, 13}, 100, 100));
        combinedPad.addVirtualButton(new VirtualButton("r_soft", "↘", new int[]{15, 13}, 100, 100));
        combinedPad.addVirtualButton(new VirtualButton("hold", "C", new int[]{4},  100, 100));
        combinedPad.addVirtualButton(new VirtualButton("rotate_ccw", "Z", new int[]{2},  100, 100));
        combinedPad.addVirtualButton(new VirtualButton("rotate_cw", "X", new int[]{3},  100, 100));
        combinedPad.addVirtualButton(new VirtualButton("rotate_180", "A", new int[]{0},  100, 100));
        combinedPad.addVirtualButton(new VirtualButton("hard_drop", "□", new int[]{1},  100, 100));
    }

    private void createUtilityControls(FrameLayout parent) {
        btnVisibilityToggle = new Button(this);
        btnVisibilityToggle.setText("●");
        FrameLayout.LayoutParams visParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        visParams.gravity = Gravity.TOP | Gravity.LEFT;
        visParams.setMargins(dpToPx(15), dpToPx(15), 0, 0);
        btnVisibilityToggle.setLayoutParams(visParams);
        btnVisibilityToggle.setBackgroundColor(Color.parseColor("#66000000"));
        btnVisibilityToggle.setTextColor(Color.WHITE);

        btnEsc = new Button(this);
        btnEsc.setText("×");
        FrameLayout.LayoutParams escParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        escParams.gravity = Gravity.TOP | Gravity.LEFT;
        escParams.setMargins(dpToPx(73), dpToPx(15), 0, 0);
        btnEsc.setLayoutParams(escParams);
        btnEsc.setBackgroundColor(Color.parseColor("#66000000"));
        btnEsc.setTextColor(Color.WHITE);

        btnR = new Button(this);
        btnR.setText("R");
        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        rParams.gravity = Gravity.TOP | Gravity.LEFT;
        rParams.setMargins(dpToPx(131), dpToPx(15), 0, 0);
        btnR.setLayoutParams(rParams);
        btnR.setBackgroundColor(Color.parseColor("#66000000"));
        btnR.setTextColor(Color.WHITE);

        // [최적화] 리스너 내부 익명 개체 생성을 최적의 인터페이스 매핑 Lambda로 간소화
        View.OnTouchListener utilityTouchListener = (v, event) -> {
            WebView webView = getCurrentWebView();
            if (webView == null) return false;

            int keyCode = (v == btnEsc) ? KeyEvent.KEYCODE_ESCAPE : KeyEvent.KEYCODE_R;
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, keyCode);
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, keyCode);
                return true;
            }
            return false;
        };

        btnEsc.setOnTouchListener(utilityTouchListener);
        btnR.setOnTouchListener(utilityTouchListener);

        btnEditToggle = new Button(this);
        btnEditToggle.setText("Edit");
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dpToPx(75), dpToPx(50));
        toggleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toggleParams.setMargins(0, dpToPx(15), dpToPx(15), 0);
        btnEditToggle.setLayoutParams(toggleParams);
        btnEditToggle.setBackgroundColor(Color.parseColor("#66000000"));
        btnEditToggle.setTextColor(Color.WHITE);

        sizeBar = new LinearLayout(this);
        sizeBar.setOrientation(LinearLayout.HORIZONTAL);
        sizeBar.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams sizeBarParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(50));
        sizeBarParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        sizeBarParams.setMargins(0, dpToPx(15), 0, 0);
        sizeBar.setLayoutParams(sizeBarParams);
        sizeBar.setBackgroundColor(Color.parseColor("#DD222222"));
        sizeBar.setPadding(dpToPx(15), 0, dpToPx(15), 0);
        sizeBar.setVisibility(View.GONE);

        Button btnMinus = new Button(this);
        btnMinus.setText("-");
        btnMinus.setTextColor(Color.WHITE);
        btnMinus.setBackgroundColor(Color.parseColor("#66000000"));
        LinearLayout.LayoutParams btnSizeParams = new LinearLayout.LayoutParams(dpToPx(45), dpToPx(35));
        btnMinus.setLayoutParams(btnSizeParams);

        tvScale = new TextView(this);
        tvScale.setText("크기: 100%");
        tvScale.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(dpToPx(15), 0, dpToPx(15), 0);
        tvScale.setLayoutParams(textParams);

        Button btnPlus = new Button(this);
        btnPlus.setText("+");
        btnPlus.setTextColor(Color.WHITE);
        btnPlus.setBackgroundColor(Color.parseColor("#66000000"));
        btnPlus.setLayoutParams(btnSizeParams);

        sizeBar.addView(btnMinus);
        sizeBar.addView(tvScale);
        sizeBar.addView(btnPlus);

        btnPlus.setOnClickListener(v -> resizeSelectedButton(0.1f));
        btnMinus.setOnClickListener(v -> resizeSelectedButton(-0.1f));
        btnEditToggle.setOnClickListener(v -> handleEditToggleTouch());
        btnVisibilityToggle.setOnClickListener(v -> handleVisibilityToggleTouch());

        parent.addView(btnVisibilityToggle);
        parent.addView(btnEsc);
        parent.addView(btnR);
        parent.addView(btnEditToggle);
        parent.addView(sizeBar);
    }
    
    private void handleVisibilityToggleTouch() {
        isPadVisible = !isPadVisible;
        if (!isPadVisible) {
            btnVisibilityToggle.setText("○");
            if (isEditMode) {
                isEditMode = false;
                btnEditToggle.setText("Edit");
                sizeBar.setVisibility(View.GONE);
                clearSelectedButton();
            }
            if (combinedPad != null) combinedPad.clearAllPressedStates();

            btnEditToggle.setVisibility(View.GONE);
            btnEsc.setVisibility(View.GONE);
            btnR.setVisibility(View.GONE);
        } else {
            btnVisibilityToggle.setText("●");
            btnEditToggle.setVisibility(View.VISIBLE);
            btnEsc.setVisibility(View.VISIBLE);
            btnR.setVisibility(View.VISIBLE);
        }
        if (combinedPad != null) combinedPad.setPadVisible(isPadVisible);
    }

    private void handleEditToggleTouch() {
        if (!isPadVisible) return;

        if (isEditMode) {
            isEditMode = false;
            btnEditToggle.setText("Edit");
            sizeBar.setVisibility(View.GONE);
            clearSelectedButton();
            if (combinedPad != null) {
                combinedPad.saveLayoutsToPrefs();
                combinedPad.invalidate();
            }
        } else {
            isEditMode = true;
            btnEditToggle.setText("Save");
            if (combinedPad != null) {
                combinedPad.clearAllPressedStates();
                combinedPad.invalidate();
            }
            clearSelectedButton();
            sizeBar.setVisibility(View.GONE);
        }
    }

    private void selectButton(VirtualButton btn) {
        if (selectedButton != null) selectedButton.selected = false;
        selectedButton = btn;
        selectedButton.selected = true;
        sizeBar.setVisibility(View.VISIBLE);
        refreshScaleText();
        if (combinedPad != null) combinedPad.invalidate();
    }

    private void clearSelectedButton() {
        if (selectedButton != null) {
            selectedButton.selected = false;
            selectedButton = null;
        }
    }

    private void refreshScaleText() {
        if (selectedButton != null && tvScale != null) {
            tvScale.setText("크기: " + Math.round(selectedButton.scaleFactor * 100) + "%");
        }
    }

    private void resizeSelectedButton(float delta) {
        if (selectedButton == null || combinedPad == null) return;

        selectedButton.scaleFactor = clamp(selectedButton.scaleFactor + delta, 0.5f, 2.0f);
        selectedButton.setBounds(selectedButton.bounds.left, selectedButton.bounds.top);
        refreshScaleText();
        combinedPad.invalidate();
    }

    private WebView getCurrentWebView() {
        return (getBridge() == null) ? null : getBridge().getWebView();
    }

    private void simulateUserGesture(WebView webView) {
        if (hasDispatchedGesture || webView == null) return;
        hasDispatchedGesture = true;

        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1f, 1f, 0);
        MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, 1f, 1f, 0);

        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        upEvent.recycle();
    }

    private void pressButton(WebView webView, VirtualButton btn) {
        if (btn == null) return;
        simulateUserGesture(webView);

        btn.activePointerCount++;
        if (btn.activePointerCount == 1) {
            btn.isCurrentPressed = true;
            for (int index : btn.gamepadButtonIndices) {
                sendGamepadStateToJs(index, true);
            }
        }
    }

    private void releaseButton(WebView webView, VirtualButton btn) {
        if (btn == null) return;

        if (btn.activePointerCount > 0) btn.activePointerCount--;
        if (btn.activePointerCount == 0 && btn.isCurrentPressed) {
            btn.isCurrentPressed = false;
            for (int index : btn.gamepadButtonIndices) {
                sendGamepadStateToJs(index, false);
            }
        }
    }

    // [최적화] 힙 메모리 재할당 연산 루프 차단 -> 사전 베이킹된 캐시 스트링 추출 전송 구조
    private void sendGamepadStateToJs(int btnIndex, boolean isPressed) {
        if (nativePort != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (btnIndex >= 0 && btnIndex < 16) {
                try {
                    // 동적 연산(Concat) 없이 다이렉트로 메모리 상주 객체 전송
                    String msg = gpMessageCache[btnIndex][isPressed ? 1 : 0];
                    nativePort.postMessage(new WebMessage(msg));
                } catch (Exception e) {
                    Log.e(TAG, "IPC 포트 전송 오류");
                }
            }
        }
    }

    private void sendNativeKeyEvent(WebView webView, int keyAction, int androidKeyCode) {
        webView.dispatchKeyEvent(new KeyEvent(keyAction, androidKeyCode));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * displayDensity);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // [최적화] 가독성 향상 및 멸실 보정 로직 고도화
    private void injectGamepadScript(WebView webView) {
        String js = "(function() {" +
                "  if (window.virtualGamepadInjected) return;" +
                "  window.virtualGamepadInjected = true;" +
                "  const mockGamepad = {" +
                "    id: 'Standard Wireless Controller (Tetroid Virtual)'," +
                "    index: 0, connected: true, timestamp: performance.now(), mapping: 'standard'," +
                "    axes: [0, 0, 0, 0]," +
                "    buttons: Array.from({ length: 16 }, () => ({ pressed: false, touched: false, value: 0 }))" +
                "  };" +
                "  navigator.getGamepads = function() {" +
                "    mockGamepad.timestamp = performance.now();" +
                "    return [mockGamepad, null, null, null];" +
                "  };" +
                "  let gamepadConnectedDispatched = false;" +
                "  function dispatchConnect() {" +
                "    if (gamepadConnectedDispatched) return;" +
                "    gamepadConnectedDispatched = true;" +
                "    try {" +
                "      window.dispatchEvent(new GamepadEvent('gamepadconnected', { gamepad: mockGamepad }));" +
                "    } catch (e) {" +
                "      const event = new Event('gamepadconnected');" +
                "      event.gamepad = mockGamepad;" +
                "      window.dispatchEvent(event);" +
                "    }" +
                "  }" +
                "  window.addEventListener('message', function(event) {" +
                "    let data;" +
                "    try { data = JSON.parse(event.data); } catch(e) { data = event.data; }" +
                "    if (data && data.type === 'INIT_PAD_PORT') {" +
                "      const port = event.ports[0];" +
                "      port.onmessage = function(e) {" +
                "        try {" +
                "          const btnData = JSON.parse(e.data);" +
                "          if (mockGamepad.buttons[btnData.btnIndex]) {" +
                "            mockGamepad.buttons[btnData.btnIndex].pressed = btnData.isPressed;" +
                "            mockGamepad.buttons[btnData.btnIndex].value = btnData.isPressed ? 1.0 : 0.0;" +
                "            if (btnData.isPressed) { dispatchConnect(); }" +
                "          }" +
                "        } catch(err) {}" +
                "      };" +
                "      dispatchConnect();" +
                "    }" +
                "  });" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private void setupMessageChannel(WebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                WebMessagePort[] channels = webView.createWebMessageChannel();
                nativePort = channels[0];
                WebMessagePort jsPort = channels[1];

                injectGamepadScript(webView);
                webView.postWebMessage(
                        new WebMessage("{\"type\":\"INIT_PAD_PORT\"}", new WebMessagePort[]{jsPort}),
                        Uri.parse("https://tetr.io")
                );
            } catch (Exception e) {
                Log.e(TAG, "IPC 빌드 실패");
            }
        }
    }

    private void optimizeWebViewPerformance(WebView webView) {
        if (webView == null) return;
        
        try {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            webView.loadUrl("https://tetr.io/" + launchRoomCode);

            android.webkit.WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setLoadsImagesAutomatically(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

            if (getWindow() != null) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
        } catch (Exception e) {
            Log.e(TAG, "최적화 셋업 실패", e);
        }
    }
}