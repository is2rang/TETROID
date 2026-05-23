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
    private static final int GRID_SIZE_DP = 10; // 10dp 그리드 격자 스냅

    // 모드 제어 플래그
    private boolean isPadVisible = true; 
    private boolean isEditMode = false;
    private GameButton selectedButton = null;
    
    // 탑 유틸리티 컴포넌트
    private LinearLayout sizeBar;
    private TextView tvScale;
    private Button btnEditToggle;
    private Button btnVisibilityToggle;

    // 복합 인풋 패킷 어레이 지원 매크로 확장형 커스텀 버튼 클래스
    private class GameButton extends androidx.appcompat.widget.AppCompatButton {
        final int[] androidKeyCodes; // 다중 동시 입력을 처리하기 위한 배열형 키 스토어
        boolean isCurrentPressed = false; 
        
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
            Log.e(TAG, "시스템 환경 구성 에러: " + e.getMessage());
        }

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupAdvancedModularSystem();
                } catch (Exception e) {
                    Log.e(TAG, "모듈러 인프라 시스템 구성 실패: " + e.getMessage(), e);
                }
            }
        });
    }

    private void setupAdvancedModularSystem() {
        FrameLayout rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        final FrameLayout combinedPad = new FrameLayout(this);
        combinedPad.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        combinedPad.setBackgroundColor(Color.parseColor("#11000000")); 

        // 1. [좌측 상단 고정] UI 토글 버튼 (심플 아이콘 변경: ● / ○)
        btnVisibilityToggle = new Button(this);
        btnVisibilityToggle.setText("●");
        FrameLayout.LayoutParams visParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(45));
        visParams.gravity = Gravity.TOP | Gravity.LEFT;
        visParams.setMargins(dpToPx(15), dpToPx(15), 0, 0);
        btnVisibilityToggle.setLayoutParams(visParams);
        btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222")); 
        btnVisibilityToggle.setTextColor(Color.WHITE);
        btnVisibilityToggle.setTextSize(16);

        // 2. [고정 유틸] ESC 단축키 버튼 (토글 모드와 무관하게 상시 오픈)
        final Button btnEsc = new Button(this);
        btnEsc.setText("ESC");
        FrameLayout.LayoutParams escParams = new FrameLayout.LayoutParams(dpToPx(55), dpToPx(45));
        escParams.gravity = Gravity.TOP | Gravity.LEFT;
        escParams.setMargins(dpToPx(15 + 50 + 8), dpToPx(15), 0, 0); // 토글 버튼 우측 정렬
        btnEsc.setLayoutParams(escParams);
        btnEsc.setBackgroundColor(Color.parseColor("#CC222222"));
        btnEsc.setTextColor(Color.WHITE);
        btnEsc.setTextSize(11);

        // 3. [고정 유틸] R 단축키 버튼 (리트라이용 상시 오픈)
        final Button btnR = new Button(this);
        btnR.setText("R");
        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(dpToPx(55), dpToPx(45));
        rParams.gravity = Gravity.TOP | Gravity.LEFT;
        rParams.setMargins(dpToPx(15 + 50 + 8 + 55 + 8), dpToPx(15), 0, 0); // ESC 버튼 우측 정렬
        btnR.setLayoutParams(rParams);
        btnR.setBackgroundColor(Color.parseColor("#CC222222"));
        btnR.setTextColor(Color.WHITE);
        btnR.setTextSize(14);

        // 상시 오픈 고정 유틸 버튼들을 위한 독점 터치 이벤트 리스너 바인딩 (호버 엔진 방해 차단)
        View.OnTouchListener utilityTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null || getBridge().getWebView() == null) return false;
                WebView webView = getBridge().getWebView();
                int keyCode = (v == btnEsc) ? KeyEvent.KEYCODE_ESCAPE : KeyEvent.KEYCODE_R;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#88FFFFFF"));
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, keyCode);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(Color.parseColor("#CC222222"));
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, keyCode);
                    return true;
                }
                return false;
            }
        };
        btnEsc.setOnTouchListener(utilityTouchListener);
        btnR.setOnTouchListener(utilityTouchListener);

        // 4. [우측 상단 고정] 편집 모드 토글 버튼
        btnEditToggle = new Button(this);
        btnEditToggle.setText("편집 Mode: OFF");
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dpToPx(130), dpToPx(45));
        toggleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toggleParams.setMargins(0, dpToPx(15), dpToPx(15), 0);
        btnEditToggle.setLayoutParams(toggleParams);
        btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); 
        btnEditToggle.setTextColor(Color.WHITE);
        btnEditToggle.setTextSize(14);

        // 5. [상단 중앙] 크기 변경 툴바 (SizeBar)
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

        // 6. [10키 아키텍처 인스턴스화] 단일 및 복합 매크로 키 바인딩 데이터 분기 처리
        GameButton btnLeft = createGameButton("◀", new int[]{KeyEvent.KEYCODE_DPAD_LEFT}, 70, 70);
        GameButton btnSoftDrop = createGameButton("▼", new int[]{KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);
        GameButton btnRight = createGameButton("▶", new int[]{KeyEvent.KEYCODE_DPAD_RIGHT}, 70, 70);
        
        // 콤보 매크로 키 (이동코드 + 소프트드롭 코드를 동시에 내부 어레이 기판에 빌드)
        GameButton btnLSoft = createGameButton("◀▼", new int[]{KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);
        GameButton btnRSoft = createGameButton("▶▼", new int[]{KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN}, 70, 70);

        GameButton btnHold = createGameButton("H", new int[]{KeyEvent.KEYCODE_C}, 70, 70);
        GameButton btnRotateCCW = createGameButton("↺", new int[]{KeyEvent.KEYCODE_Z}, 70, 70);
        GameButton btnRotateCW = createGameButton("↻", new int[]{KeyEvent.KEYCODE_DPAD_UP}, 70, 70);
        GameButton btnRotate180 = createGameButton("180", new int[]{KeyEvent.KEYCODE_A}, 70, 70); // 180도 회전 바인딩 (일반적 TETR.IO 기본 설정값인 A키 매핑)
        GameButton btnHardDrop = createGameButton("DROP", new int[]{KeyEvent.KEYCODE_SPACE}, 90, 70);

        // 7. [인체공학형 2열 입체 배치 마진 맵]
        // 왼손 구역 (상단열: 기본 방향 이동 / 하단열: 대각선 복합 매크로 배치)
        setButtonInitialLayout(btnLeft, Gravity.BOTTOM | Gravity.LEFT, 20, 20 + 70 + 10);      // 2열 상단
        setButtonInitialLayout(btnSoftDrop, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10, 20 + 70 + 10);
        setButtonInitialLayout(btnRight, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10 + 70 + 10, 20 + 70 + 10);
        setButtonInitialLayout(btnLSoft, Gravity.BOTTOM | Gravity.LEFT, 20, 20);               // 1열 하단
        setButtonInitialLayout(btnRSoft, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 10 + 70 + 10, 20);

        // 오른손 구역 (상단열: 특수 액션 및 홀드 / 하단열: 주력 회전 및 드롭 배치)
        setButtonInitialLayout(btnHardDrop, Gravity.BOTTOM | Gravity.RIGHT, 20, 20);          // 1열 하단
        setButtonInitialLayout(btnRotateCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10, 20);
        setButtonInitialLayout(btnRotateCCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10, 20);
        setButtonInitialLayout(btnRotate180, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10, 20 + 70 + 10); // 2열 상단
        setButtonInitialLayout(btnHold, Gravity.BOTTOM | Gravity.RIGHT, 20 + 90 + 10 + 70 + 10, 20 + 70 + 10);

        // 컴포넌트 적재
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
        
        combinedPad.addView(btnEditToggle);
        combinedPad.addView(btnVisibilityToggle);
        combinedPad.addView(btnEsc);
        combinedPad.addView(btnR);
        combinedPad.addView(sizeBar);

        setupIntegratedHoverEngine(combinedPad);
        setupEditModeInteraction(combinedPad);

        // [관통 제어 스위치 고도화 리스너]
        btnVisibilityToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPadVisible = !isPadVisible;
                if (!isPadVisible) {
                    btnVisibilityToggle.setText("○"); // 꺼짐 심볼 동기화
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC0055AA")); 
                    combinedPad.setBackgroundColor(Color.TRANSPARENT); 
                    
                    isEditMode = false;
                    btnEditToggle.setText("편집 Mode: OFF");
                    btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000"));
                    sizeBar.setVisibility(View.GONE);
                    if (selectedButton != null) {
                        selectedButton.setAlpha(1.0f);
                        selectedButton = null;
                    }
                    clearHoverOperationalStates(combinedPad);

                    // ●, ESC, R 버튼을 제외한 모든 인게임 인풋 요소를 완전히 숨겨 순정 터치 유도
                    int count = combinedPad.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View child = combinedPad.getChildAt(i);
                        if (child != btnVisibilityToggle && child != btnEsc && child != btnR) {
                            child.setVisibility(View.GONE);
                        }
                    }
                    Log.d(TAG, "인게임 조작계가 봉인되어 100% 터치 관통 모드로 전향되었습니다.");
                } else {
                    btnVisibilityToggle.setText("●"); // 켜짐 심볼 동기화
                    btnVisibilityToggle.setBackgroundColor(Color.parseColor("#CC222222"));
                    combinedPad.setBackgroundColor(Color.parseColor("#11000000"));

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
                if (!isPadVisible) return; 
                
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

    // 마스터 멀티터치 실시간 프레임 호버링 엔진
    private void setupIntegratedHoverEngine(final FrameLayout pad) {
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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

                // [매크로 연산 동기화 루프]
                for (int j = 0; j < childCount; j++) {
                    View child = pad.getChildAt(j);
                    if (child instanceof GameButton) {
                        GameButton btn = (GameButton) child;
                        boolean isCurrentlyHovered = nextFrameStates[j];

                        if (isCurrentlyHovered && !btn.isCurrentPressed) {
                            btn.isCurrentPressed = true;
                            btn.setBackgroundColor(Color.parseColor("#BBFFFFFF"));
                            // 해당 매크로 버튼 내부에 할당된 모든 키 신호를 배열 순서대로 연속 순정 주입 (KeyDown)
                            for (int code : btn.androidKeyCodes) {
                                sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, code);
                            }
                        } else if (!isCurrentlyHovered && btn.isCurrentPressed) {
                            btn.isCurrentPressed = false;
                            btn.setBackgroundColor(Color.parseColor("#66FFFFFF"));
                            // 해당 매크로 버튼 내부에 할당된 모든 키 신호를 배열 순서대로 연속 순정 주입 (KeyUp)
                            for (int code : btn.androidKeyCodes) {
                                sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
                            }
                        }
                    }
                }
                return true; 
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
                    for (int code : btn.androidKeyCodes) {
                        sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, code);
                    }
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
