package com.is2rang.tetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "TetrioMobile";
    private static final int GRID_SIZE_DP = 10;
    private static final String PREFS_NAME = "TetroidCustomPadPrefs";
    private static final int MAX_CACHED_KEY_CODE = 1023;

    private String launchRoomCode = "";

    private boolean isPadVisible = true;
    private boolean isEditMode = false;

    private VirtualButton selectedButton = null;

    private float displayDensity;

    private GamePadOverlay combinedPad;

    private final int[] keyPressRefCounts = new int[MAX_CACHED_KEY_CODE + 1];
    private final KeyEvent[] downEvents = new KeyEvent[MAX_CACHED_KEY_CODE + 1];
    private final KeyEvent[] upEvents = new KeyEvent[MAX_CACHED_KEY_CODE + 1];

    private LinearLayout sizeBar;
    private TextView tvScale;
    private Button btnEditToggle;
    private Button btnVisibilityToggle;
    private Button btnEsc;
    private Button btnR;

    private class VirtualButton {
        final String saveKey;
        final String label;
        final RectF bounds = new RectF();
        final Rect hitRect = new Rect();
        final int[] androidKeyCodes;

        boolean isCurrentPressed = false;
        int activePointerCount = 0;
        boolean selected = false;

        final int baseWidthDp;
        final int baseHeightDp;
        float scaleFactor = 1.0f;

        VirtualButton(String saveKey, String label, int[] keyCodes, int baseWidthDp, int baseHeightDp) {
            this.saveKey = saveKey;
            this.label = label;
            this.androidKeyCodes = keyCodes;
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
        private static final int HITMAP_CELL_SIZE_DP = 20;

        private final Paint buttonPaint = new Paint();
        private final Paint selectedBorderPaint = new Paint();
        private final Paint textPaint = new Paint();

        private final List<VirtualButton> buttons = new ArrayList<>();

        private VirtualButton[] pointerButtonMap = new VirtualButton[16];
        private int[] pointerCellXMap = new int[16];
        private int[] pointerCellYMap = new int[16];

        private int hitMapCellSizePx;
        private int hitMapCols = 0;
        private int hitMapRows = 0;
        private VirtualButton[] hitMap = new VirtualButton[0];
        private boolean hitMapDirty = true;

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
            setClipChildren(false);
            setClipToPadding(false);

            hitMapCellSizePx = Math.max(1, dpToPx(HITMAP_CELL_SIZE_DP));
            Arrays.fill(pointerCellXMap, -1);
            Arrays.fill(pointerCellYMap, -1);

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
            markHitMapDirty();
        }

        List<VirtualButton> getButtons() {
            return buttons;
        }

        void markHitMapDirty() {
            hitMapDirty = true;
        }

        void setPadVisible(boolean visible) {
            if (!visible) {
                releaseAllPressedStates();
            }
            postInvalidateOnAnimation();
        }

        void initializeButtonLayouts() {
            if (layoutInitialized) {
                return;
            }

            if (getWidth() == 0 || getHeight() == 0) {
                post(this::initializeButtonLayouts);
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            for (VirtualButton btn : buttons) {
                applyStoredOrDefaultLayout(btn, prefs);
            }

            layoutInitialized = true;
            markHitMapDirty();
            postInvalidateOnAnimation();
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
                case "left":
                    btn.setBounds(leftX, bottomY);
                    break;
                case "soft_drop":
                    btn.setBounds(midX, bottomY);
                    break;
                case "right":
                    btn.setBounds(rightX, bottomY);
                    break;
                case "l_soft":
                    btn.setBounds(leftX, upperY);
                    break;
                case "r_soft":
                    btn.setBounds(rightX, upperY);
                    break;
                case "hard_drop":
                    btn.setBounds(rightGroupRightX, bottomY);
                    break;
                case "rotate_cw":
                    btn.setBounds(rightGroupMidX, bottomY);
                    break;
                case "rotate_ccw":
                    btn.setBounds(rightGroupLeftX, bottomY);
                    break;
                case "rotate_180":
                    btn.setBounds(rightGroupMidX, upperY);
                    break;
                case "hold":
                    btn.setBounds(rightGroupLeftX, upperY);
                    break;
                default:
                    btn.setBounds(leftX, bottomY);
                    break;
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

        void clearAllPressedStates() {
            WebView webView = getCurrentWebView();

            if (webView == null) {
                for (VirtualButton btn : buttons) {
                    btn.activePointerCount = 0;
                    btn.isCurrentPressed = false;
                }

                Arrays.fill(keyPressRefCounts, 0);
                Arrays.fill(pointerButtonMap, null);
                Arrays.fill(pointerCellXMap, -1);
                Arrays.fill(pointerCellYMap, -1);
                postInvalidateOnAnimation();
                return;
            }

            for (int i = 0; i < pointerButtonMap.length; i++) {
                VirtualButton btn = pointerButtonMap[i];
                if (btn != null) {
                    releaseButton(webView, btn);
                    pointerButtonMap[i] = null;
                }
                pointerCellXMap[i] = -1;
                pointerCellYMap[i] = -1;
            }

            postInvalidateOnAnimation();
        }

        private void releaseAllPressedStates() {
            clearAllPressedStates();
        }

        private void ensurePointerCapacity(int pointerId) {
            if (pointerId < pointerButtonMap.length) {
                return;
            }

            int newSize = pointerButtonMap.length;
            while (newSize <= pointerId) {
                newSize *= 2;
            }

            pointerButtonMap = Arrays.copyOf(pointerButtonMap, newSize);
            pointerCellXMap = Arrays.copyOf(pointerCellXMap, newSize);
            pointerCellYMap = Arrays.copyOf(pointerCellYMap, newSize);

            Arrays.fill(pointerCellXMap, hitMapCols == 0 ? -1 : -1);
            Arrays.fill(pointerCellYMap, hitMapRows == 0 ? -1 : -1);
        }

        private void setPointerButton(int pointerId, VirtualButton btn) {
            ensurePointerCapacity(pointerId);
            pointerButtonMap[pointerId] = btn;
        }

        private VirtualButton getPointerButton(int pointerId) {
            if (pointerId < 0 || pointerId >= pointerButtonMap.length) {
                return null;
            }
            return pointerButtonMap[pointerId];
        }

        private int getPointerCellX(int pointerId) {
            if (pointerId < 0 || pointerId >= pointerCellXMap.length) {
                return -1;
            }
            return pointerCellXMap[pointerId];
        }

        private int getPointerCellY(int pointerId) {
            if (pointerId < 0 || pointerId >= pointerCellYMap.length) {
                return -1;
            }
            return pointerCellYMap[pointerId];
        }

        private void setPointerCell(int pointerId, int cellX, int cellY) {
            ensurePointerCapacity(pointerId);
            pointerCellXMap[pointerId] = cellX;
            pointerCellYMap[pointerId] = cellY;
        }

        private void clearPointerButton(int pointerId) {
            if (pointerId < 0 || pointerId >= pointerButtonMap.length) {
                return;
            }
            pointerButtonMap[pointerId] = null;
            pointerCellXMap[pointerId] = -1;
            pointerCellYMap[pointerId] = -1;
        }

        private void rebuildHitMapIfNeeded() {
            if (!hitMapDirty) {
                return;
            }

            int width = getWidth();
            int height = getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            hitMapCellSizePx = Math.max(1, dpToPx(HITMAP_CELL_SIZE_DP));
            hitMapCols = Math.max(1, (width + hitMapCellSizePx - 1) / hitMapCellSizePx);
            hitMapRows = Math.max(1, (height + hitMapCellSizePx - 1) / hitMapCellSizePx);
            hitMap = new VirtualButton[hitMapCols * hitMapRows];

            for (int i = 0; i < buttons.size(); i++) {
                VirtualButton btn = buttons.get(i);
                if (btn == null) {
                    continue;
                }

                int leftCell = Math.max(0, (int) (btn.bounds.left / hitMapCellSizePx));
                int topCell = Math.max(0, (int) (btn.bounds.top / hitMapCellSizePx));
                int rightCell = Math.min(hitMapCols - 1, (int) ((btn.bounds.right - 1) / hitMapCellSizePx));
                int bottomCell = Math.min(hitMapRows - 1, (int) ((btn.bounds.bottom - 1) / hitMapCellSizePx));

                for (int row = topCell; row <= bottomCell; row++) {
                    int rowBase = row * hitMapCols;
                    for (int col = leftCell; col <= rightCell; col++) {
                        hitMap[rowBase + col] = btn;
                    }
                }
            }

            hitMapDirty = false;
        }

        private VirtualButton findButtonAt(float x, float y) {
            rebuildHitMapIfNeeded();

            if (hitMap.length == 0 || hitMapCols <= 0 || hitMapRows <= 0) {
                return null;
            }

            int cellX = (int) (x / hitMapCellSizePx);
            int cellY = (int) (y / hitMapCellSizePx);

            if (cellX < 0 || cellY < 0 || cellX >= hitMapCols || cellY >= hitMapRows) {
                return null;
            }

            VirtualButton btn = hitMap[cellY * hitMapCols + cellX];
            if (btn != null && btn.hitRect.contains((int) x, (int) y)) {
                return btn;
            }

            return null;
        }

        private boolean handleEditTouch(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    VirtualButton btn = findButtonAt(event.getX(), event.getY());
                    if (btn == null) {
                        return true;
                    }

                    selectButton(btn);

                    dragStartX = event.getX();
                    dragStartY = event.getY();
                    originLeft = btn.bounds.left;
                    originTop = btn.bounds.top;
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (selectedButton == null) {
                        return true;
                    }

                    float dx = event.getX() - dragStartX;
                    float dy = event.getY() - dragStartY;

                    float newLeft = originLeft + dx;
                    float newTop = originTop + dy;

                    float gridPx = dpToPx(GRID_SIZE_DP);

                    newLeft = Math.round(newLeft / gridPx) * gridPx;
                    newTop = Math.round(newTop / gridPx) * gridPx;

                    float maxLeft = Math.max(0, getWidth() - selectedButton.getWidthPx());
                    float maxTop = Math.max(0, getHeight() - selectedButton.getHeightPx());

                    newLeft = clamp(newLeft, 0f, maxLeft);
                    newTop = clamp(newTop, 0f, maxTop);

                    selectedButton.setBounds(newLeft, newTop);
                    markHitMapDirty();
                    postInvalidateOnAnimation();
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }

            return true;
        }

        private boolean handleGameTouch(MotionEvent event) {
            WebView webView = getCurrentWebView();
            if (webView == null) {
                return false;
            }

            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int pointerId = event.getPointerId(actionIndex);
                    float x = event.getX(actionIndex);
                    float y = event.getY(actionIndex);

                    VirtualButton btn = findButtonAt(x, y);
                    setPointerButton(pointerId, btn);

                    int cellX = (int) (x / hitMapCellSizePx);
                    int cellY = (int) (y / hitMapCellSizePx);
                    setPointerCell(pointerId, cellX, cellY);

                    pressButton(webView, btn);
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    int pointerCount = event.getPointerCount();

                    for (int i = 0; i < pointerCount; i++) {
                        int pointerId = event.getPointerId(i);
                        float x = event.getX(i);
                        float y = event.getY(i);

                        int cellX = (int) (x / hitMapCellSizePx);
                        int cellY = (int) (y / hitMapCellSizePx);

                        int oldCellX = getPointerCellX(pointerId);
                        int oldCellY = getPointerCellY(pointerId);

                        if (cellX == oldCellX && cellY == oldCellY) {
                            continue;
                        }

                        VirtualButton oldBtn = getPointerButton(pointerId);
                        VirtualButton newBtn = findButtonAt(x, y);

                        if (oldBtn != newBtn) {
                            releaseButton(webView, oldBtn);
                            pressButton(webView, newBtn);
                            setPointerButton(pointerId, newBtn);
                        }

                        setPointerCell(pointerId, cellX, cellY);
                    }

                    return true;
                }

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP: {
                    int pointerId = event.getPointerId(actionIndex);
                    VirtualButton btn = getPointerButton(pointerId);

                    releaseButton(webView, btn);
                    clearPointerButton(pointerId);
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

            if (!isPadVisible) {
                return;
            }

            for (VirtualButton btn : buttons) {
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
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            hitMapDirty = true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isPadVisible) {
                return false;
            }

            if (isEditMode) {
                return handleEditTouch(event);
            }

            return handleGameTouch(event);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String roomCode = getIntent().getStringExtra("roomCode");
        launchRoomCode = roomCode != null ? roomCode : "";

        initKeyEventCache();

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

        displayDensity = getResources().getDisplayMetrics().density;

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
            Log.e(TAG, "해상도 몰입 모드 초기화 실패: " + e.getMessage(), e);
        }

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupAdvancedModularSystem();
                } catch (Exception e) {
                    Log.e(TAG, "인프라 시스템 주입 실패: " + e.getMessage(), e);
                }
            }
        });
    }

    private void initKeyEventCache() {
        cacheKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
        cacheKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
        cacheKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
        cacheKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
        cacheKeyEvent(KeyEvent.KEYCODE_C);
        cacheKeyEvent(KeyEvent.KEYCODE_Z);
        cacheKeyEvent(KeyEvent.KEYCODE_A);
        cacheKeyEvent(KeyEvent.KEYCODE_SPACE);
        cacheKeyEvent(KeyEvent.KEYCODE_ESCAPE);
        cacheKeyEvent(KeyEvent.KEYCODE_R);
    }

    private void cacheKeyEvent(int keyCode) {
        if (!isValidKeyCode(keyCode)) {
            return;
        }

        downEvents[keyCode] = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        upEvents[keyCode] = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
    }

    private boolean isValidKeyCode(int keyCode) {
        return keyCode >= 0 && keyCode <= MAX_CACHED_KEY_CODE;
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

        combinedPad.post(new Runnable() {
            @Override
            public void run() {
                combinedPad.initializeButtonLayouts();
            }
        });
    }

    private void createVirtualButtons() {
        if (combinedPad == null) return;

        combinedPad.addVirtualButton(new VirtualButton(
                "left",
                "←",
                new int[]{KeyEvent.KEYCODE_DPAD_LEFT},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "soft_drop",
                "↓",
                new int[]{KeyEvent.KEYCODE_DPAD_DOWN},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "right",
                "→",
                new int[]{KeyEvent.KEYCODE_DPAD_RIGHT},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "l_soft",
                "↙",
                new int[]{KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "r_soft",
                "↘",
                new int[]{KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "hold",
                "C",
                new int[]{KeyEvent.KEYCODE_C},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "rotate_ccw",
                "Z",
                new int[]{KeyEvent.KEYCODE_Z},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "rotate_cw",
                "X",
                new int[]{KeyEvent.KEYCODE_DPAD_UP},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "rotate_180",
                "A",
                new int[]{KeyEvent.KEYCODE_A},
                100,
                100
        ));

        combinedPad.addVirtualButton(new VirtualButton(
                "hard_drop",
                "□",
                new int[]{KeyEvent.KEYCODE_SPACE},
                100,
                100
        ));
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
        btnVisibilityToggle.setTextSize(16);

        btnEsc = new Button(this);
        btnEsc.setText("×");
        FrameLayout.LayoutParams escParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        escParams.gravity = Gravity.TOP | Gravity.LEFT;
        escParams.setMargins(dpToPx(15 + 50 + 8), dpToPx(15), 0, 0);
        btnEsc.setLayoutParams(escParams);
        btnEsc.setBackgroundColor(Color.parseColor("#66000000"));
        btnEsc.setTextColor(Color.WHITE);
        btnEsc.setTextSize(16);

        btnR = new Button(this);
        btnR.setText("R");
        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        rParams.gravity = Gravity.TOP | Gravity.LEFT;
        rParams.setMargins(dpToPx(15 + 50 + 8 + 50 + 8), dpToPx(15), 0, 0);
        btnR.setLayoutParams(rParams);
        btnR.setBackgroundColor(Color.parseColor("#66000000"));
        btnR.setTextColor(Color.WHITE);
        btnR.setTextSize(14);

        View.OnTouchListener utilityTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WebView webView = getCurrentWebView();
                if (webView == null) return false;

                int keyCode = v == btnEsc ? KeyEvent.KEYCODE_ESCAPE : KeyEvent.KEYCODE_R;

                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, keyCode);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, keyCode);
                    return true;
                }

                return false;
            }
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
        btnEditToggle.setTextSize(13);

        sizeBar = new LinearLayout(this);
        sizeBar.setOrientation(LinearLayout.HORIZONTAL);
        sizeBar.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams sizeBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(50)
        );
        sizeBarParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        sizeBarParams.setMargins(0, dpToPx(15), 0, 0);
        sizeBar.setLayoutParams(sizeBarParams);
        sizeBar.setBackgroundColor(Color.parseColor("#DD222222"));
        sizeBar.setPadding(dpToPx(15), 0, dpToPx(15), 0);
        sizeBar.setVisibility(View.GONE);

        Button btnMinus = new Button(this);
        btnMinus.setText("-");
        btnMinus.setTextSize(18);
        btnMinus.setTextColor(Color.WHITE);
        btnMinus.setBackgroundColor(Color.parseColor("#66000000"));
        LinearLayout.LayoutParams btnSizeParams = new LinearLayout.LayoutParams(dpToPx(45), dpToPx(35));
        btnMinus.setLayoutParams(btnSizeParams);

        tvScale = new TextView(this);
        tvScale.setText("크기: 100%");
        tvScale.setTextColor(Color.WHITE);
        tvScale.setTextSize(14);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(dpToPx(15), 0, dpToPx(15), 0);
        tvScale.setLayoutParams(textParams);

        Button btnPlus = new Button(this);
        btnPlus.setText("+");
        btnPlus.setTextSize(18);
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

            clearHoverOperationalStates();

            btnEditToggle.setVisibility(View.GONE);
            btnEsc.setVisibility(View.GONE);
            btnR.setVisibility(View.GONE);
        } else {
            btnVisibilityToggle.setText("●");
            btnEditToggle.setVisibility(View.VISIBLE);
            btnEsc.setVisibility(View.VISIBLE);
            btnR.setVisibility(View.VISIBLE);
        }

        if (combinedPad != null) {
            combinedPad.setPadVisible(isPadVisible);
        }
    }

    private void handleEditToggleTouch() {
        if (!isPadVisible) return;

        if (isEditMode) {
            isEditMode = false;
            btnEditToggle.setText("Edit");
            sizeBar.setVisibility(View.GONE);

            if (selectedButton != null) {
                selectedButton.selected = false;
                selectedButton = null;
            }

            executeSaveCurrentLayouts();
            if (combinedPad != null) {
                combinedPad.postInvalidateOnAnimation();
            }
        } else {
            isEditMode = true;
            btnEditToggle.setText("Save");
            clearHoverOperationalStates();

            if (selectedButton != null) {
                selectedButton.selected = false;
                selectedButton = null;
            }

            sizeBar.setVisibility(View.GONE);
            if (combinedPad != null) {
                combinedPad.postInvalidateOnAnimation();
            }
        }
    }

    private void executeSaveCurrentLayouts() {
        if (combinedPad == null) return;
        combinedPad.saveLayoutsToPrefs();
    }

    private void clearHoverOperationalStates() {
        if (combinedPad != null) {
            combinedPad.clearAllPressedStates();
        }
    }

    private void selectButton(VirtualButton btn) {
        if (selectedButton != null) {
            selectedButton.selected = false;
        }

        selectedButton = btn;
        selectedButton.selected = true;
        sizeBar.setVisibility(View.VISIBLE);
        refreshScaleText();

        if (combinedPad != null) {
            combinedPad.postInvalidateOnAnimation();
        }
    }

    private void clearSelectedButton() {
        if (selectedButton != null) {
            selectedButton.selected = false;
            selectedButton = null;
        }
    }

    private void refreshScaleText() {
        if (selectedButton != null && tvScale != null) {
            int ratio = Math.round(selectedButton.scaleFactor * 100);
            tvScale.setText("크기: " + ratio + "%");
        }
    }

    private void resizeSelectedButton(float delta) {
        if (selectedButton == null || combinedPad == null) {
            return;
        }

        selectedButton.scaleFactor += delta;
        selectedButton.scaleFactor = clamp(selectedButton.scaleFactor, 0.5f, 2.0f);

        float left = selectedButton.bounds.left;
        float top = selectedButton.bounds.top;

        selectedButton.setBounds(left, top);
        combinedPad.markHitMapDirty();
        refreshScaleText();
        combinedPad.postInvalidateOnAnimation();
    }

    private WebView getCurrentWebView() {
        if (getBridge() == null) return null;
        return getBridge().getWebView();
    }

    private void pressButton(WebView webView, VirtualButton btn) {
        if (btn == null) return;

        btn.activePointerCount++;

        if (btn.activePointerCount == 1) {
            btn.isCurrentPressed = true;

            for (int code : btn.androidKeyCodes) {
                incrementKeyRefAndMaybeDispatchDown(webView, code);
            }
        }
    }

    private void releaseButton(WebView webView, VirtualButton btn) {
        if (btn == null) return;

        if (btn.activePointerCount > 0) {
            btn.activePointerCount--;
        }

        if (btn.activePointerCount == 0 && btn.isCurrentPressed) {
            btn.isCurrentPressed = false;

            for (int code : btn.androidKeyCodes) {
                decrementKeyRefAndMaybeDispatchUp(webView, code);
            }
        }
    }

    private void incrementKeyRefAndMaybeDispatchDown(WebView webView, int androidKeyCode) {
        if (webView == null || !isValidKeyCode(androidKeyCode)) {
            return;
        }

        int count = keyPressRefCounts[androidKeyCode];
        keyPressRefCounts[androidKeyCode] = count + 1;

        if (count == 0) {
            sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, androidKeyCode);
        }
    }

    private void decrementKeyRefAndMaybeDispatchUp(WebView webView, int androidKeyCode) {
        if (webView == null || !isValidKeyCode(androidKeyCode)) {
            return;
        }

        int count = keyPressRefCounts[androidKeyCode];
        if (count <= 0) {
            return;
        }

        count--;
        keyPressRefCounts[androidKeyCode] = count;

        if (count == 0) {
            sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, androidKeyCode);
        }
    }

    private void sendNativeKeyEvent(WebView webView, int keyAction, int androidKeyCode) {
        if (webView == null) return;

        if (isValidKeyCode(androidKeyCode)) {
            KeyEvent cached = (keyAction == KeyEvent.ACTION_DOWN)
                    ? downEvents[androidKeyCode]
                    : upEvents[androidKeyCode];

            if (cached == null) {
                cached = new KeyEvent(keyAction, androidKeyCode);
                if (keyAction == KeyEvent.ACTION_DOWN) {
                    downEvents[androidKeyCode] = cached;
                } else {
                    upEvents[androidKeyCode] = cached;
                }
            }

            webView.dispatchKeyEvent(cached);
            return;
        }

        webView.dispatchKeyEvent(new KeyEvent(keyAction, androidKeyCode));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * displayDensity);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void optimizeWebViewPerformance(WebView webView) {
        if (webView == null) return;

        try {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            android.webkit.WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setLoadsImagesAutomatically(true);
            settings.setMediaPlaybackRequiresUserGesture(false);

            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            webView.requestFocus();
            webView.setFocusable(true);
            webView.setFocusableInTouchMode(true);

            webView.loadUrl("https://tetr.io/" + launchRoomCode);

            if (getWindow() != null) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "웹뷰 가속 엔진 빌드 실패: " + e.getMessage(), e);
        }
    }
}