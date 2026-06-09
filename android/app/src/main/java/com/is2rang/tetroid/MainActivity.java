package com.is2rang.tetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "TetrioMobile";
    private static final int GRID_SIZE_DP = 10;
    private static final String PREFS_NAME = "TetroidCustomPadPrefs";

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
    private Button btnEnter;

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
            return dpToPx(baseWidthDp) * scaleFactor;
        }

        float getHeightPx() {
            return dpToPx(baseHeightDp) * scaleFactor;
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
            setClipChildren(false);
            setClipToPadding(false);

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
                releaseAllPressedStates();
            }
            invalidate();
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
                pointerButtonMap.clear();
                return;
            }

            HashSet<VirtualButton> uniqueButtons = new HashSet<>();
            for (int i = 0; i < pointerButtonMap.size(); i++) {
                VirtualButton btn = pointerButtonMap.valueAt(i);
                if (btn != null) uniqueButtons.add(btn);
            }

            for (VirtualButton btn : uniqueButtons) {
                if (btn.isCurrentPressed) {
                    btn.isCurrentPressed = false;
                    btn.activePointerCount = 0;
                    for (int code : btn.androidKeyCodes) {
                        sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
                    }
                }
            }

            pointerButtonMap.clear();
            invalidate();
        }

        private void releaseAllPressedStates() {
            clearAllPressedStates();
        }

        private VirtualButton findButtonAt(float x, float y) {
            for (int i = 0; i < buttons.size(); i++) {
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
                    invalidate();
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

            if (!isPadVisible) {
                return;
            }

            for (VirtualButton btn : buttons) {
                canvas.drawRect(btn.bounds, buttonPaint);

                float cx = btn.bounds.centerX();
                float cy = btn.bounds.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f);
                canvas.drawText(btn.label, cx, cy, textPaint);
            }
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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

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
        setupEditModeInteraction();

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

        btnEnter = new Button(this);
        btnEnter.setText("<");
        FrameLayout.LayoutParams enterParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50));
        enterParams.gravity = Gravity.TOP | Gravity.LEFT;
        enterParams.setMargins(dpToPx(15 + 50 + 8 + 50 + 8 + 50 + 8), dpToPx(15), 0, 0);
        btnEnter.setLayoutParams(enterParams);
        btnEnter.setBackgroundColor(Color.parseColor("#66000000"));
        btnEnter.setTextColor(Color.WHITE);
        btnEnter.setTextSize(14);

        View.OnTouchListener utilityTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WebView webView = getCurrentWebView();
                if (webView == null) return false;

                int keyCode;
                if (v == btnEsc) {
                    keyCode = KeyEvent.KEYCODE_ESCAPE;
                } else if (v == btnR) {
                    keyCode = KeyEvent.KEYCODE_R;
                } else {
                    keyCode = KeyEvent.KEYCODE_ENTER;
                }

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
        btnEnter.setOnTouchListener(utilityTouchListener);

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

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resizeSelectedButton(0.1f);
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resizeSelectedButton(-0.1f);
            }
        });

        parent.addView(btnVisibilityToggle);
        parent.addView(btnEsc);
        parent.addView(btnR);
        parent.addView(btnEnter);
        parent.addView(btnEditToggle);
        parent.addView(sizeBar);

        btnVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    btnEnter.setVisibility(View.GONE);
                } else {
                    btnVisibilityToggle.setText("●");
                    btnEditToggle.setVisibility(View.VISIBLE);
                    btnEsc.setVisibility(View.VISIBLE);
                    btnR.setVisibility(View.VISIBLE);
                    btnEnter.setVisibility(View.VISIBLE);

                    if (combinedPad != null) {
                        combinedPad.setPadVisible(true);
                        combinedPad.invalidate();
                    }
                }

                if (combinedPad != null) {
                    combinedPad.setPadVisible(isPadVisible);
                }
            }
        });
    }

    private void setupEditModeInteraction() {
        btnEditToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        combinedPad.invalidate();
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
                        combinedPad.invalidate();
                    }
                }
            }
        });
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
            combinedPad.invalidate();
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
        refreshScaleText();
        combinedPad.invalidate();
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
                sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, code);
            }
        }

        if (combinedPad != null) {
            combinedPad.invalidate();
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
                sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
            }
        }

        if (combinedPad != null) {
            combinedPad.invalidate();
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

    private void optimizeWebViewPerformance(WebView webView) {
        if (webView == null) return;

        try {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            webView.setWebViewClient(new com.getcapacitor.BridgeWebViewClient(getBridge()) {
                @Override
                public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                    String url = request.getUrl().toString();

                    if (url.contains("googleads") ||
                            url.contains("doubleclick") ||
                            url.contains("adnxs") ||
                            url.contains("adservice") ||
                            url.contains("pagead")) {

                        return new android.webkit.WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                new java.io.ByteArrayInputStream("".getBytes())
                        );
                    }

                    return super.shouldInterceptRequest(view, request);
                }
            });

            android.webkit.WebSettings settings = webView.getSettings();

            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setLoadsImagesAutomatically(true);
            settings.setMediaPlaybackRequiresUserGesture(false);

            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

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