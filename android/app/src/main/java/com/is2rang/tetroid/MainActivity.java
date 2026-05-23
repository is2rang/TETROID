package com.is2rang.tetroid;

import android.content.Context;
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

public class MainActivity extends BridgeActivity {
    private static final String TAG = "TetrioMobile";
    private static final int GRID_SIZE_DP = 10; // 그리드 스냅 격자 크기

    // 시스템 모드 제어 플래그
    private boolean isPadVisible = true; // 패드 UI 노출 및 터치 작동 여부
    private boolean isEditMode = false;
    private GameButton selectedButton = null;
    
    // 상단 UI 컴포넌트 선언
    private LinearLayout sizeBar;
    private TextView tvScale;
    private Button btnEditToggle;
    private Button btnVisibilityToggle;

    // 크기 및 키코드 확장 커스텀 버튼 클래스
    private class GameButton extends androidx.appcompat.widget.AppCompatButton {
        final int androidKeyCode;
        boolean isCurrentPressed = false; 
        
        int baseWidthDp;
        int baseHeightDp;
        float scaleFactor = 1.0f;

        public GameButton(Context context, int androidKeyCode, int baseWidthDp, int baseHeightDp) {
            super(context);
            this.androidKeyCode = androidKeyCode;
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

        try {
            // 가로 고정 및 몰입 모드 초기화
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
            Log.e(TAG, "UI 초기화 실패: " + e.getMessage());
        }

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupAdvancedModularSystem();
                } catch (Exception e) {
                    Log.e(TAG, "모듈러 제어 레이어 구성 실패: " + e.getMessage(), e);
                }
            }
        });
    }

    private void setupAdvancedModularSystem() {
        FrameLayout rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        // 화면 전체를 덮는 통합 베이스 도화지
        final FrameLayout combinedPad = new FrameLayout(this);
        combinedPad.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        combinedPad.setBackgroundColor(Color.parseColor("#11000000")); 

        // 1. [좌측 상단 고정] 패드 UI 숨김/보이기 토글 버튼 (언제나 터치 가능 상태 유지)
        btnVisibilityToggle = new Button(this);
        btnVisibilityToggle.setText("UI 숨기기");
        FrameLayout.LayoutParams visParams = new FrameLayout.LayoutParams(dpToPx(130), dpToPx(45));
        visParams.gravity = Gravity.TOP | Gravity.LEFT;
        visParams.setMargins(dpToPx(15), dpToPx(15), 0, 0);
        btnVisibilityToggle.setLayoutParams(visParams);
        btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222")); // 시크한 차콜 다크 톤
        btnVisibilityToggle.setTextColor(Color.WHITE);
        btnVisibilityToggle.setTextSize(14);

        // 2. [우측 상단 고정] 편집 모드 On/Off 토글 버튼
        btnEditToggle = new Button(this);
        btnEditToggle.setText("편집 Mode: OFF");
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dpToPx(130), dpToPx(45));
        toggleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toggleParams.setMargins(0, dpToPx(15), dpToPx(15), 0);
        btnEditToggle.setLayoutParams(toggleParams);
        btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); 
        btnEditToggle.setTextColor(Color.WHITE);
        btnEditToggle.setTextSize(14);

        // 3. [상단 중앙] 크기 조절 툴바 (SizeBar)
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

        // 인게임 가상 버튼 레이어 정형화 생성 (10dp리팩토링 규격)
        GameButton btnLeft = createGameButton("◀", KeyEvent.KEYCODE_DPAD_LEFT, 70, 70);
        GameButton btnSoftDrop = createGameButton("▼", KeyEvent.KEYCODE_DPAD_DOWN, 70, 70);
        GameButton btnRight = createGameButton("▶", KeyEvent.KEYCODE_DPAD_RIGHT, 70, 70);
        GameButton btnHold = createGameButton("H", KeyEvent.KEYCODE_C, 70, 70);
        GameButton btnRotateCCW = createGameButton("↺", KeyEvent.KEYCODE_Z, 70, 70);
        GameButton btnRotateCW = createGameButton("↻", KeyEvent.KEYCODE_DPAD_UP, 70, 70);
        GameButton btnHardDrop = createGameButton("DROP", KeyEvent.KEYCODE_SPACE, 90, 70);

        // 그리드 맞춤용 마진 초기 세팅
        setButtonInitialLayout(btnLeft, Gravity.BOTTOM | Gravity.LEFT, 20, 20);
        setButtonInitialLayout(btnSoftDrop, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10, 20);      
        setButtonInitialLayout(btnRight, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10 + 70 + 10, 20);

        setButtonInitialLayout(btnHardDrop, Gravity.BOTTOM | Gravity.RIGHT, 20, 20);
        setButtonInitialLayout(btnRotateCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10, 20);
        setButtonInitialLayout(btnRotateCCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10, 20);
        setButtonInitialLayout(btnHold, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10 + 70 + 10, 20);

        // 도화지에 컴포넌트 전체 적재
        combinedPad.addView(btnLeft);
        combinedPad.addView(btnSoftDrop);
        combinedPad.addView(btnRight);
        combinedPad.addView(btnHold);
        combinedPad.addView(btnRotateCCW);
        combinedPad.addView(btnRotateCW);
        combinedPad.addView(btnHardDrop);
        combinedPad.addView(btnEditToggle);
        combinedPad.addView(btnVisibilityToggle); // 고정형 숨김 토글 장착
        combinedPad.addView(sizeBar);

        // 비즈니스 로직 시스템 엔진 작동
        setupIntegratedHoverEngine(combinedPad);
        setupEditModeInteraction(combinedPad);

        // [핵심 구현] 패드 UI 온오프 관통 시스템 스위치 액션 리스너
        btnVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPadVisible = !isPadVisible;
                if (!isPadVisible) {
                    // 패드 숨기기 모드 활성화
                    btnVisibilityToggle.setText("UI 보이기");
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC0055AA")); // 산뜻한 블루 톤 변환
                    combinedPad.setBackgroundColor(Color.TRANSPARENT); // 배경 완전 클리어
                    
                    // 강제 예외 처리: 편집 모드 무조건 종료 및 리셋
                    isEditMode = false;
                    btnEditToggle.setText("편집 Mode: OFF");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000"));
                    sizeBar.setVisibility(View.GONE);
                    if (selectedButton != null) {
                        selectedButton.setAlpha(1.0f);
                        selectedButton = null;
                    }
                    clearHoverOperationalStates(combinedPad);

                    // 토글 버튼 본인을 제외한 모든 가상 자식 요소들을 화면에서 영구 증발시킴
                    int count = combinedPad.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View child = combinedPad.getChildAt(i);
                        if (child != btnVisibilityToggle) {
                            child.setVisibility(View.GONE);
                        }
                    }
                    Log.d(TAG, "가상 패드가 비활성화되어 안드로이드 순정 터치 관통 모드로 전환되었습니다.");
                } else {
                    // 패드 보이기 모드 복원
                    btnVisibilityToggle.setText("UI 숨기기");
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222"));
                    combinedPad.setBackgroundColor(Color.parseColor("#11000000"));

                    // 기본 조작 패드 버튼 및 편집 토글 복원
                    int count = combinedPad.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View child = combinedPad.getChildAt(i);
                        if (child instanceof GameButton || child == btnEditToggle) {
                            child.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        // 플러스 마이너스 버튼 액션 스케일 바인딩
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

    private void setButtonInitialLayout(GameButton btn, int gravity, int marginXDp, int marginYDp) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dpToPx(btn.baseWidthDp), dpToPx(btn.baseHeightDp));
        lp.gravity = gravity;
        if ((gravity & Gravity.LEFT) == Gravity.LEFT) lp.leftMargin = dpToPx(marginXDp);
        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) lp.rightMargin = dpToPx(marginXDp);
        if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) lp.bottomMargin = dpToPx(marginYDp);
        if ((gravity & Gravity.TOP) == Gravity.TOP) lp.topMargin = dpToPx(marginYDp);
        btn.setLayoutParams(lp);
    }

    private GameButton createGameButton(String text, final int androidKeyCode, int widthDp, int heightDp) {
        final GameButton button = new GameButton(this, androidKeyCode, widthDp, heightDp);
        button.setText(text);
        button.setTextSize(16);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#66FFFFFF"));
        button.setTextColor(Color.BLACK);
        button.setClickable(false); 

        button.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private int initLeft, initBottom, initRight, initTop;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPadVisible || !isEditMode) return false; // 숨김 상태거나 편집 모드가 아닐 때 탈출

                GameButton btn = (GameButton) v;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btn.getLayoutParams();
                int gridSizePx = dpToPx(GRID_SIZE_DP);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        executeButtonSelection(btn);
                        startX = event.getRawX();
                        startY = event.getRawY();
                        initLeft = lp.leftMargin;
                        initBottom = lp.bottomMargin;
                        initRight = lp.rightMargin;
                        initTop = lp.topMargin;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float diffX = event.getRawX() - startX;
                        float diffY = event.getRawY() - startY;

                        if ((lp.gravity & Gravity.LEFT) == Gravity.LEFT) {
                            int targetLeft = initLeft + (int) diffX;
                            lp.leftMargin = Math.round((float) targetLeft / gridSizePx) * gridSizePx;
                        } else if ((lp.gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                            int targetRight = initRight - (int) diffX;
                            lp.rightMargin = Math.round((float) targetRight / gridSizePx) * gridSizePx;
                        }

                        if ((lp.gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                            int targetBottom = initBottom - (int) diffY;
                            lp.bottomMargin = Math.round((float) targetBottom / gridSizePx) * gridSizePx;
                        } else if ((lp.gravity & Gravity.TOP) == Gravity.TOP) {
                            int targetTop = initTop + (int) diffY;
                            lp.topMargin = Math.round((float) targetTop / gridSizePx) * gridSizePx;
                        }

                        btn.setLayoutParams(lp);
                        break;
                }
                return true; 
            }
        });

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
                if (!isPadVisible) return; // 패드가 안 보일 때는 작동 금지
                
                isEditMode = !isEditMode;
                if (isEditMode) {
                    btnEditToggle.setText("편집 Mode: ON");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CC00AA00")); 
                    clearHoverOperationalStates(pad);
                } else {
                    btnEditToggle.setText("편집 Mode: OFF");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); 
                    sizeBar.setVisibility(View.GONE);
                    if (selectedButton != null) {
                        selectedButton.setAlpha(1.0f);
                        selectedButton = null;
                    }
                }
            }
        });
    }

    // 마스터 멀티터치 인게임 호버 엔진
    private void setupIntegratedHoverEngine(final FrameLayout pad) {
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // [웹뷰 관통 분기 처리의 핵심] 숨김 모드이거나 편집 모드일 경우 터치를 독점하지 않고 즉시 Pass 시킴
                if (!isPadVisible || isEditMode) return false; 

                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getActionMasked();
                int pointerCount = event.getPointerCount();
                int childCount = pad.getChildCount();

                boolean[] nextFrameStates = new boolean[childCount];

                if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
                    for (int i = 0; i < pointerCount; i++) {
                        if (action == MotionEvent.ACTION_POINTER_UP && i == event.getActionIndex()) {
                            continue;
                        }

                        float x = event.getX(i);
                        float y = event.getY(i);

                        for (int j = 0; j < childCount; j++) {
                            View child = pad.getChildAt(j);
                            if (child instanceof GameButton && child.getVisibility() == View.VISIBLE) {
                                if (x >= child.getLeft() && x <= child.getRight() &&
                                    y >= child.getTop() && y <= child.getBottom()) {
                                    nextFrameStates[j] = true;
                                }
                            }
                        }
                    }
                }

                for (int j = 0; j < childCount; j++) {
                    View child = pad.getChildAt(j);
                    if (child instanceof GameButton) {
                        GameButton btn = (GameButton) child;
                        boolean isCurrentlyHovered = nextFrameStates[j];

                        if (isCurrentlyHovered && !btn.isCurrentPressed) {
                            btn.isCurrentPressed = true;
                            btn.setBackgroundColor(Color.parseColor("#BBFFFFFF"));
                            sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, btn.androidKeyCode);
                        } else if (!isCurrentlyHovered && btn.isCurrentPressed) {
                            btn.isCurrentPressed = false;
                            btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                            sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, btn.androidKeyCode);
                        }
                    }
                }
                return true; // 인게임 플레이 중에는 터치를 독점하여 웹뷰 클릭 씹힘 및 맵 스크롤을 원천 차단
            }
        });
    }

    private void clearHoverOperationalStates(FrameLayout pad) {
        if (getBridge() == null || getBridge().getWebView() == null) return;
        WebView webView = getBridge().getWebView();
        int childCount = pad.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = pad.getChildAt(i);
            if (v instanceof GameButton) {
                GameButton btn = (GameButton) v;
                if (btn.isCurrentPressed) {
                    btn.isCurrentPressed = false;
                    btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, btn.androidKeyCode);
                }
            }
        }
    }

    private void sendNativeKeyEvent(final WebView webView, final int keyAction, final int androidKeyCode) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.requestFocus();
                KeyEvent keyEvent = new KeyEvent(keyAction, androidKeyCode);
                webView.dispatchKeyEvent(keyEvent);
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
