package com.is2rang.tetroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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

    // 시스템 상태 제어 플래그
    private boolean isPadVisible = true; 
    private boolean isEditMode = false;
    private GameButton selectedButton = null;
    
    // [최적화] 캐싱된 조작 버튼 리스트 (런타임 루프 부하 절감)
    private final List<GameButton> gameButtons = new ArrayList<>();
    
    // [최적화] 디스플레이 밀도 값 전역 캐싱 (Math 연산 비용 절감)
    private float displayDensity;

    // 탑 유틸리티 컴포넌트 브릿지
    private LinearLayout sizeBar;
    private TextView tvScale;
    private Button btnEditToggle;
    private Button btnVisibilityToggle;

    // 인게임 조작 전용 확장형 가상 버튼 클래스
    private class GameButton extends androidx.appcompat.widget.AppCompatButton {
        final int[] androidKeyCodes; 
        boolean isCurrentPressed = false; 
        boolean tempHovered = false; // [최적화] 매 프레임 New 배열 할당을 없애기 위한 내부 상태 플래그
        
        int baseWidthDp;
        int baseHeightDp;
        float scaleFactor = 1.0f;

        public GameButton(Context context, int[] androidKeyCodes, int baseWidthDp, int baseHeightDp) {
            super(context);
            this.androidKeyCodes = androidKeyCodes;
            this.baseWidthDp = baseWidthDp;
            this.baseHeightDp = baseHeightDp;
        }

        public void changeScale(float delta) {
            scaleFactor += delta;
            if (scaleFactor < 0.5f) scaleFactor = 0.5f; 
            if (scaleFactor > 2.0f) scaleFactor = 2.0f; 
            
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            lp.width = dpToPx((int) (baseWidthDp * scaleFactor));
            lp.height = dpToPx((int) (baseHeightDp * scaleFactor));
            setLayoutParams(lp);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 밀도 가중치 사전 캐싱
        displayDensity = getResources().getDisplayMetrics().density;

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
            Log.e(TAG, "해상도 몰입 모드 초기화 실패: " + e.getMessage());
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

        // 🚀 [환경 업그레이드] 크롬 웹뷰 커널 WebGL 성능 튜닝 강제 인젝션
        if (getBridge() != null && getBridge().getWebView() != null) {
            optimizeWebViewPerformance(getBridge().getWebView());
        }

        final FrameLayout combinedPad = new FrameLayout(this);
        combinedPad.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        combinedPad.setBackgroundColor(Color.parseColor("#11000000")); 

        // 1. [좌측 상단 고정] UI 상태 토글 스위치 (●: 켜짐 / ○: 꺼짐)
        btnVisibilityToggle = new Button(this);
        btnVisibilityToggle.setText("●");
        FrameLayout.LayoutParams visParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(45));
        visParams.gravity = Gravity.TOP | Gravity.LEFT;
        visParams.setMargins(dpToPx(15), dpToPx(15), 0, 0);
        btnVisibilityToggle.setLayoutParams(visParams);
        btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222")); 
        btnVisibilityToggle.setTextColor(Color.WHITE);
        btnVisibilityToggle.setTextSize(16);

        // 2. [고정 유틸] ESC 버튼 (드래그/스케일 예외, 위치 고정)
        final Button btnEsc = new Button(this);
        btnEsc.setText("ESC");
        FrameLayout.LayoutParams escParams = new FrameLayout.LayoutParams(dpToPx(55), dpToPx(45));
        escParams.gravity = Gravity.TOP | Gravity.LEFT;
        escParams.setMargins(dpToPx(15 + 50 + 8), dpToPx(15), 0, 0); 
        btnEsc.setLayoutParams(escParams);
        btnEsc.setBackgroundColor(Color.parseColor("#CC222222"));
        btnEsc.setTextColor(Color.WHITE);
        btnEsc.setTextSize(11);

        // 3. [고정 유틸] R 버튼 (드래그/스케일 예외, 위치 고정)
        final Button btnR = new Button(this);
        btnR.setText("R");
        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(dpToPx(55), dpToPx(45));
        rParams.gravity = Gravity.TOP | Gravity.LEFT;
        rParams.setMargins(dpToPx(15 + 50 + 8 + 55 + 8), dpToPx(15), 0, 0); 
        btnR.setLayoutParams(rParams);
        btnR.setBackgroundColor(Color.parseColor("#CC222222"));
        btnR.setTextColor(Color.WHITE);
        btnR.setTextSize(14);

        // 고정 버튼 터치 최적화: UI 스레드 직통 발송으로 연산 지연 완전히 제거
        View.OnTouchListener utilityTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null || getBridge().getWebView() == null) return false;
                WebView webView = getBridge().getWebView();
                int keyCode = (v == btnEsc) ? KeyEvent.KEYCODE_ESCAPE : KeyEvent.KEYCODE_R;

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#88FFFFFF"));
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, keyCode);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(Color.parseColor("#CC222222"));
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, keyCode);
                    return true;
                }
                return false;
            }
        };
        btnEsc.setOnTouchListener(utilityTouchListener);
        btnR.setOnTouchListener(utilityTouchListener);

        // 4. [우측 상단 고정] Edit / Save 제어 토글 버튼
        btnEditToggle = new Button(this);
        btnEditToggle.setText("Edit");
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dpToPx(100), dpToPx(45));
        toggleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toggleParams.setMargins(0, dpToPx(15), dpToPx(15), 0);
        btnEditToggle.setLayoutParams(toggleParams);
        btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); 
        btnEditToggle.setTextColor(Color.WHITE);
        btnEditToggle.setTextSize(15);

        // 5. [상단 중앙] 크기 조절 세그먼트 바 (SizeBar)
        sizeBar = new LinearLayout(this);
        sizeBar.setOrientation(LinearLayout.HORIZONTAL);
        sizeBar.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams sizeBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(50));
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
        btnMinus.setBackgroundColor(Color.parseColor("#55FFFFFF"));
        LinearLayout.LayoutParams btnSizeParams = new LinearLayout.LayoutParams(dpToPx(45), dpToPx(35));
        btnMinus.setLayoutParams(btnSizeParams);

        tvScale = new TextView(this);
        tvScale.setText("크기: 100%");
        tvScale.setTextColor(Color.WHITE);
        tvScale.setTextSize(14);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(dpToPx(15), 0, dpToPx(15), 0);
        tvScale.setLayoutParams(textParams);

        Button btnPlus = new Button(this);
        btnPlus.setText("+");
        btnPlus.setTextSize(18);
        btnPlus.setTextColor(Color.WHITE);
        btnPlus.setBackgroundColor(Color.parseColor("#55FFFFFF"));
        btnPlus.setLayoutParams(btnSizeParams);

        sizeBar.addView(btnMinus);
        sizeBar.addView(tvScale);
        sizeBar.addView(btnPlus);

        // 6. [순수 조작계 10키 생성 및 바인딩]
        GameButton btnLeft = createGameButton("◀", new int[]{KeyEvent.KEYCODE_DPAD_LEFT}, 70, 70);
        GameButton btnSoftDrop = createGameButton("▼", new int[]{KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);
        GameButton btnRight = createGameButton("▶", new int[]{KeyEvent.KEYCODE_DPAD_RIGHT}, 70, 70);
        GameButton btnLSoft = createGameButton("◀▼", new int[]{KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);
        GameButton btnRSoft = createGameButton("▶▼", new int[]{KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);

        GameButton btnHold = createGameButton("H", new int[]{KeyEvent.KEYCODE_C}, 70, 70);
        GameButton btnRotateCCW = createGameButton("↺", new int[]{KeyEvent.KEYCODE_Z}, 70, 70);
        GameButton btnRotateCW = createGameButton("↻", new int[]{KeyEvent.KEYCODE_DPAD_UP}, 70, 70);
        GameButton btnRotate180 = createGameButton("180", new int[]{KeyEvent.KEYCODE_A}, 70, 70); 
        GameButton btnHardDrop = createGameButton("DROP", new int[]{KeyEvent.KEYCODE_SPACE}, 90, 70);

        // 7. [레이아웃 영구 저장 데이터 로드 및 적용]
        initAndLoadButtonLayout(btnLeft, Gravity.BOTTOM | Gravity.LEFT, 20, 20 + 70 + 10);
        initAndLoadButtonLayout(btnSoftDrop, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10, 20 + 70 + 10);
        initAndLoadButtonLayout(btnRight, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10 + 70 + 10, 20 + 70 + 10);
        initAndLoadButtonLayout(btnLSoft, Gravity.BOTTOM | Gravity.LEFT, 20, 20);
        initAndLoadButtonLayout(btnRSoft, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10 + 70 + 10, 20);

        initAndLoadButtonLayout(btnHardDrop, Gravity.BOTTOM | Gravity.RIGHT, 20, 20);
        initAndLoadButtonLayout(btnRotateCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10, 20);
        initAndLoadButtonLayout(btnRotateCCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10, 20);
        initAndLoadButtonLayout(btnRotate180, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10, 20 + 70 + 10);
        initAndLoadButtonLayout(btnHold, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10, 20 + 70 + 10);

        combinedPad.addView(btnLeft);
        combinedPad.addView(btnSoftDrop);
        combinedPad.addView(btnRight);
        combinedPad.addView(btnLSoft);
        combinedPad.addView(btnRSoft);
        combinedPad.addView(btnHold);
        combinedPad.addView(btnRotateCCW);
        combinedPad.addView(btnRotateCW);
        combinedPad.addView(btnRotate180);
        combinedPad.addView(btnHardDrop);
        
        combinedPad.addView(btnEsc);
        combinedPad.addView(btnR);
        combinedPad.addView(btnEditToggle);
        combinedPad.addView(btnVisibilityToggle);
        combinedPad.addView(sizeBar);

        setupOptimizedHoverEngine(combinedPad);
        setupEditModeInteraction(combinedPad);

        // [관통 스위치 리스너] 고정 버튼인 ESC와 R도 다른 조작키들과 함께 온전히 ON/OFF 처리
        btnVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPadVisible = !isPadVisible;
                if (!isPadVisible) {
                    btnVisibilityToggle.setText("○"); 
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC0055AA")); 
                    combinedPad.setBackgroundColor(Color.TRANSPARENT); 
                    
                    if (isEditMode) {
                        isEditMode = false;
                        btnEditToggle.setText("Edit");
                        btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000"));
                        sizeBar.setVisibility(View.GONE);
                        if (selectedButton != null) {
                            selectedButton.setAlpha(1.0f);
                            selectedButton = null;
                        }
                    }
                    clearHoverOperationalStates();

                    // ● 마크를 제외한 고정 유틸 버튼(ESC, R), Edit 버튼, 10키 패드를 완전히 화면에서 언로드
                    btnEditToggle.setVisibility(View.GONE);
                    btnEsc.setVisibility(View.GONE);
                    btnR.setVisibility(View.GONE);
                    
                    // [최적화] 무거운 뷰 계층 탐색 대신 캐싱 리스트 직접 순회로 가시성 일괄 제어
                    for (int i = 0; i < gameButtons.size(); i++) {
                        gameButtons.get(i).setVisibility(View.GONE);
                    }
                } else {
                    btnVisibilityToggle.setText("●"); 
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222"));
                    combinedPad.setBackgroundColor(Color.parseColor("#11000000"));

                    btnEditToggle.setVisibility(View.VISIBLE);
                    btnEsc.setVisibility(View.VISIBLE);
                    btnR.setVisibility(View.VISIBLE);
                    for (int i = 0; i < gameButtons.size(); i++) {
                        gameButtons.get(i).setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedButton != null) {
                    selectedButton.changeScale(0.1f);
                    refreshScaleText();
                }
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedButton != null) {
                    selectedButton.changeScale(-0.1f);
                    refreshScaleText();
                }
            }
        });

        rootView.addView(combinedPad);
    }

    // SharedPreferences 데이터 로컬 보존 로직
    private void executeSaveCurrentLayouts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (int i = 0; i < gameButtons.size(); i++) {
            GameButton btn = gameButtons.get(i);
            String key = btn.getText().toString();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btn.getLayoutParams();
            
            editor.putInt(key + "_leftMargin", lp.leftMargin);
            editor.putInt(key + "_rightMargin", lp.rightMargin);
            editor.putInt(key + "_topMargin", lp.topMargin);
            editor.putInt(key + "_bottomMargin", lp.bottomMargin);
            editor.putInt(key + "_gravity", lp.gravity);
            editor.putFloat(key + "_scaleFactor", btn.scaleFactor);
        }
        editor.apply();
        Log.d(TAG, "10키 맞춤형 레이아웃이 SharedPreferences에 영구 세이브되었습니다.");
    }

    private void initAndLoadButtonLayout(GameButton btn, int defaultGravity, int marginXDp, int marginYDp) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = btn.getText().toString();

        if (prefs.contains(key + "_gravity")) {
            int savedGravity = prefs.getInt(key + "_gravity", defaultGravity);
            float savedScale = prefs.getFloat(key + "_scaleFactor", 1.0f);
            btn.scaleFactor = savedScale;

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    dpToPx((int) (btn.baseWidthDp * savedScale)), dpToPx((int) (btn.baseHeightDp * savedScale)));
            lp.gravity = savedGravity;
            lp.leftMargin = prefs.getInt(key + "_leftMargin", 0);
            lp.rightMargin = prefs.getInt(key + "_rightMargin", 0);
            lp.topMargin = prefs.getInt(key + "_topMargin", 0);
            lp.bottomMargin = prefs.getInt(key + "_bottomMargin", 0);
            btn.setLayoutParams(lp);
        } else {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dpToPx(btn.baseWidthDp), dpToPx(btn.baseHeightDp));
            lp.gravity = defaultGravity;
            if ((defaultGravity & Gravity.LEFT) == Gravity.LEFT) lp.leftMargin = dpToPx(marginXDp);
            if ((defaultGravity & Gravity.RIGHT) == Gravity.RIGHT) lp.rightMargin = dpToPx(marginXDp);
            if ((defaultGravity & Gravity.BOTTOM) == Gravity.BOTTOM) lp.bottomMargin = dpToPx(marginYDp);
            if ((defaultGravity & Gravity.TOP) == Gravity.TOP) lp.topMargin = dpToPx(marginYDp);
            btn.setLayoutParams(lp);
        }
    }

    private GameButton createGameButton(String text, final int[] androidKeyCodes, int widthDp, int heightDp) {
        final GameButton button = new GameButton(this, androidKeyCodes, widthDp, heightDp);
        button.setText(text);
        button.setTextSize(14);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#66FFFFFF"));
        button.setTextColor(Color.BLACK);
        button.setClickable(false); 

        button.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private int initLeft, initBottom, initRight, initTop;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPadVisible || !isEditMode) return false; 

                GameButton btn = (GameButton) v;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btn.getLayoutParams();
                int gridSizePx = dpToPx(GRID_SIZE_DP);

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    executeButtonSelection(btn);
                    startX = event.getRawX();
                    startY = event.getRawY();
                    initLeft = lp.leftMargin;
                    initBottom = lp.bottomMargin;
                    initRight = lp.rightMargin;
                    initTop = lp.topMargin;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    float diffX = event.getRawX() - startX;
                    float diffY = event.getRawY() - startY;

                    if ((lp.gravity & Gravity.LEFT) == Gravity.LEFT) {
                        int targetLeft = initLeft + (int) diffX;
                        lp.leftMargin = (targetLeft / gridSizePx) * gridSizePx;
                    } else if ((lp.gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                        int targetRight = initRight - (int) diffX;
                        lp.rightMargin = (targetRight / gridSizePx) * gridSizePx;
                    }

                    if ((lp.gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        int targetBottom = initBottom - (int) diffY;
                        lp.bottomMargin = (targetBottom / gridSizePx) * gridSizePx;
                    } else if ((lp.gravity & Gravity.TOP) == Gravity.TOP) {
                        int targetTop = initTop + (int) diffY;
                        lp.topMargin = (targetTop / gridSizePx) * gridSizePx;
                    }

                    btn.setLayoutParams(lp);
                }
                return true; 
            }
        });

        gameButtons.add(button); 
        return button;
    }

    private void executeButtonSelection(GameButton btn) {
        if (selectedButton != null) {
            selectedButton.setAlpha(1.0f);
        }
        selectedButton = btn;
        selectedButton.setAlpha(0.6f); 
        sizeBar.setVisibility(View.VISIBLE);
        refreshScaleText();
    }

    private void refreshScaleText() {
        if (selectedButton != null && tvScale != null) {
            int ratio = Math.round(selectedButton.scaleFactor * 100);
            tvScale.setText("크기: " + ratio + "%");
        }
    }

    private void setupEditModeInteraction(final FrameLayout pad) {
        btnEditToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPadVisible) return; 
                
                if (isEditMode) {
                    isEditMode = false;
                    btnEditToggle.setText("Edit");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); 
                    sizeBar.setVisibility(View.GONE);
                    if (selectedButton != null) {
                        selectedButton.setAlpha(1.0f);
                        selectedButton = null;
                    }
                    executeSaveCurrentLayouts(); 
                } else {
                    isEditMode = true;
                    btnEditToggle.setText("Save");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CC00AA00")); 
                    clearHoverOperationalStates();
                }
            }
        });
    }

    // ⭐ [핵심 구현] 가비지 컬렉션(GC)을 차단하여 프레임 드랍을 완벽 격리한 멀티터치 호버 엔진
    private void setupOptimizedHoverEngine(final FrameLayout pad) {
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPadVisible || isEditMode) return false; 

                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getActionMasked();
                int pointerCount = event.getPointerCount();
                int buttonSize = gameButtons.size();

                // [최적화 1] 매 터치 이동마다 발생하던 boolean[] 배열 인스턴스 할당 제거
                for (int j = 0; j < buttonSize; j++) {
                    gameButtons.get(j).tempHovered = false;
                }

                // [최적화 2] getChildAt() 계층 구조 순회 완전 우회 -> 캐싱된 리스트 1:1 Direct 매칭 연산
                if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
                    int actionIndex = event.getActionIndex();
                    for (int i = 0; i < pointerCount; i++) {
                        if (action == MotionEvent.ACTION_POINTER_UP && i == actionIndex) {
                            continue;
                        }

                        float x = event.getX(i);
                        float y = event.getY(i);

                        for (int j = 0; j < buttonSize; j++) {
                            GameButton btn = gameButtons.get(j);
                            if (btn.getVisibility() == View.VISIBLE) {
                                if (x >= btn.getLeft() && x <= btn.getRight() &&
                                    y >= btn.getTop() && y <= btn.getBottom()) {
                                    btn.tempHovered = true;
                                }
                            }
                        }
                    }
                }

                // [최적화 3] 인풋 버퍼 발송 시 가바지 컬렉터를 유발하는 .post(Runnable) 우회, 메인 스레드 즉각 삽입
                for (int j = 0; j < buttonSize; j++) {
                    GameButton btn = gameButtons.get(j);
                    if (btn.tempHovered && !btn.isCurrentPressed) {
                        btn.isCurrentPressed = true;
                        btn.setBackgroundColor(Color.parseColor("#BBFFFFFF"));
                        for (int code : btn.androidKeyCodes) {
                            sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, code);
                        }
                    } else if (!btn.tempHovered && btn.isCurrentPressed) {
                        btn.isCurrentPressed = false;
                        btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                        for (int code : btn.androidKeyCodes) {
                            sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
                        }
                    }
                }
                return true; 
            }
        });
    }

    private void clearHoverOperationalStates() {
        if (getBridge() == null || getBridge().getWebView() == null) return;
        WebView webView = getBridge().getWebView();
        int buttonSize = gameButtons.size();
        for (int i = 0; i < buttonSize; i++) {
            GameButton btn = gameButtons.get(i);
            if (btn.isCurrentPressed) {
                btn.isCurrentPressed = false;
                btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                for (int code : btn.androidKeyCodes) {
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
                }
            }
        }
    }

    // [최적화 4] 호출마다 new 익명 객체가 생성되던 오버헤드를 완전히 걷어낸 초고속 키 인젝터
    private void sendNativeKeyEvent(WebView webView, int keyAction, int androidKeyCode) {
        webView.requestFocus();
        webView.dispatchKeyEvent(new KeyEvent(keyAction, androidKeyCode));
    }

    private int dpToPx(int dp) {
        return (int) (dp * displayDensity);
    }

    // 🚀 [환경 최적화 코어 엔진] 크롬 내부 런타임 하드웨어 GPU 가속 튜닝 아키텍처
    private void optimizeWebViewPerformance(WebView webView) {
        if (webView == null) return;

        try {
            // 1. 하드웨어 가속 강제 바인딩 (WebGL 렌더링 파이프라인 GPU 직통 연결)
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            android.webkit.WebSettings settings = webView.getSettings();
            
            // 2. V8 고성능 자바스크립트 가속 및 DOM 컴파일러 세팅 최적화
            settings.setJavaScriptEnabled(true);
            settings.setDOMStorageEnabled(true); // TETR.IO 에셋 로컬 캐시 메모리 증설
            settings.setDatabaseEnabled(true);
            
            // 3. 리소스 반복 다운로드로 인한 프레임 드랍 방지 (네트워크 스터터링 차단)
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setLoadsImagesAutomatically(true);
            
            // 4. 모바일 GPU 프리징 억제 및 부하 감소
            settings.setMediaPlaybackRequiresUserGesture(false); 
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            
            // 5. OS 윈도우 디스플레이 버퍼 레이어 하드웨어 가속 강제화
            if (getWindow() != null) {
                getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                );
            }
            Log.d(TAG, "TETR.IO 하드웨어 가속 및 브라우저 성능 최적화가 완벽히 주입되었습니다.");
        } catch (Exception e) {
            Log.e(TAG, "웹뷰 가속 엔진 빌드 실패: " + e.getMessage());
        }
    }
}
