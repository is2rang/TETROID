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

    // 시스템 상태 제어 전역 변수
    private boolean isEditMode = false;
    private GameButton selectedButton = null;
    
    // UI 컴포넌트 선언
    private LinearLayout sizeBar;
    private TextView tvScale;

    // 개별 버튼의 고유 스케일 및 원본 크기 저장을 위한 커스텀 클래스
    private class GameButton extends androidx.appcompat.widget.AppCompatButton {
        final int androidKeyCode;
        boolean isCurrentPressed = false; 
        
        // 크기 편집용 원본 치수(dp) 및 스케일 팩터
        int baseWidthDp;
        int baseHeightDp;
        float scaleFactor = 1.0f;

        public GameButton(Context context, int androidKeyCode, int baseWidthDp, int baseHeightDp) {
            super(context);
            this.androidKeyCode = androidKeyCode;
            this.baseWidthDp = baseWidthDp;
            this.baseHeightDp = baseHeightDp;
        }

        // 실시간 크기 연산 및 적용 함수
        public void changeScale(float delta) {
            scaleFactor += delta;
            if (scaleFactor < 0.5f) scaleFactor = 0.5f; // 최소 크기 제한 (50%)
            if (scaleFactor > 2.0f) scaleFactor = 2.0f; // 최대 크기 제한 (200%)
            
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
            // 가로 모드 및 소프트키 전체 숨김 몰입 모드 활성화
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
            Log.e(TAG, "초기 화면 환경 세팅 에러: " + e.getMessage());
        }

        // 화면 렌더 트리가 안전하게 안착된 후 통합 뷰 레이아웃 주입
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupAdvancedModularSystem();
                } catch (Exception e) {
                    Log.e(TAG, "모듈러 시스템 주입 실패: " + e.getMessage(), e);
                }
            }
        });
    }

    // 게임패드, 편집 시스템, 크기 제어 툴바 통합 생성 프로세스
    private void setupAdvancedModularSystem() {
        FrameLayout rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        // 1. 최상위 베이스 도화지 레이아웃 (단일 통합 프레임)
        final FrameLayout combinedPad = new FrameLayout(this);
        combinedPad.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // 2. [고정 노출] 오른쪽 상단 편집 모드 On/Off 토글 버튼 설계
        final Button btnEditToggle = new Button(this);
        btnEditToggle.setText("편집 Mode: OFF");
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(dpToPx(130), dpToPx(45));
        toggleParams.gravity = Gravity.TOP | Gravity.RIGHT;
        toggleParams.setMargins(0, dpToPx(15), dpToPx(15), 0);
        btnEditToggle.setLayoutParams(toggleParams);
        btnEditToggle.setBackgroundColor(Color.parseColor("#CCAA0000")); // 초기 레드 톤
        btnEditToggle.setTextColor(Color.WHITE);
        btnEditToggle.setTextSize(14);

        // 3. [상단 중앙] 크기 조절 바(SizeBar) 동적 레이아웃 설계 (기본 숨김)
        sizeBar = new LinearLayout(this);
        sizeBar.setOrientation(LinearLayout.HORIZONTAL);
        sizeBar.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams sizeBarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(50));
        sizeBarParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        sizeBarParams.setMargins(0, dpToPx(15), 0, 0);
        sizeBar.setLayoutParams(sizeBarParams);
        sizeBar.setBackgroundColor(Color.parseColor("#DD222222")); // 불투명 다크 톤
        sizeBar.setPadding(dpToPx(15), 0, dpToPx(15), 0);
        sizeBar.setVisibility(View.GONE); // 기본 비활성화

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

        // 4. 가상 인게임 조작 버튼 인스턴스 레이어 규격화 생성
        GameButton btnLeft = createGameButton("◀", KeyEvent.KEYCODE_DPAD_LEFT, 70, 75);
        GameButton btnSoftDrop = createGameButton("▼", KeyEvent.KEYCODE_DPAD_DOWN, 70, 75);
        GameButton btnRight = createGameButton("▶", KeyEvent.KEYCODE_DPAD_RIGHT, 70, 75);
        GameButton btnHold = createGameButton("H", KeyEvent.KEYCODE_C, 65, 75);
        GameButton btnRotateCCW = createGameButton("↺", KeyEvent.KEYCODE_Z, 65, 75);
        GameButton btnRotateCW = createGameButton("↻", KeyEvent.KEYCODE_DPAD_UP, 65, 75);
        GameButton btnHardDrop = createGameButton("DROP", KeyEvent.KEYCODE_SPACE, 85, 75);

        // 5. 초기 배치 좌표 설정 (Gravity 구조 매핑)
        setButtonInitialLayout(btnLeft, Gravity.BOTTOM | Gravity.LEFT, 20, 20);
        setButtonInitialLayout(btnSoftDrop, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 6, 20);
        setButtonInitialLayout(btnRight, Gravity.BOTTOM | Gravity.LEFT, 20 + 70 + 6 + 70 + 6, 20);
        
        setButtonInitialLayout(btnHardDrop, Gravity.BOTTOM | Gravity.RIGHT, 20, 20);
        setButtonInitialLayout(btnRotateCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 85 + 6, 20);
        setButtonInitialLayout(btnRotateCCW, Gravity.BOTTOM | Gravity.RIGHT, 20 + 85 + 6 + 65 + 6, 20);
        setButtonInitialLayout(btnHold, Gravity.BOTTOM | Gravity.RIGHT, 20 + 85 + 6 + 65 + 6 + 65 + 6, 20);

        // 6. 컴포넌트 조립 순서 제어 (버튼 -> 제어 툴바 가독성 확보)
        combinedPad.addView(btnLeft);
        combinedPad.addView(btnSoftDrop);
        combinedPad.addView(btnRight);
        combinedPad.addView(btnHold);
        combinedPad.addView(btnRotateCCW);
        combinedPad.addView(btnRotateCW);
        combinedPad.addView(btnHardDrop);
        combinedPad.addView(btnEditToggle);
        combinedPad.addView(sizeBar);

        // 7. 통합 엔진 구동 및 리스너 인터록 설계
        setupIntegratedHoverEngine(combinedPad);
        setupEditModeInteraction(btnEditToggle, combinedPad);

        // 크기 조절 세부 액션 리스너 정의
        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedButton != null) {
                    selectedButton.changeScale(0.1f); // 10% 확대
                    refreshScaleText();
                }
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedButton != null) {
                    selectedButton.changeScale(-0.1f); // 10% 축소
                    refreshScaleText();
                }
            }
        });

        rootView.addView(combinedPad);
        Log.d(TAG, "모듈러 커스텀 시스템 인터페이스 배치 완결.");
    }

    // 버튼 초기 레이아웃 구조체 규격 설정 헬퍼 함수
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

    // 마스터 가상 버튼 빌더 및 멀티 모드(플레이/편집 드래그) 상호작용 제어기
    private GameButton createGameButton(String text, final int androidKeyCode, int widthDp, int heightDp) {
        final GameButton button = new GameButton(this, androidKeyCode, widthDp, heightDp);
        button.setText(text);
        button.setTextSize(16);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#66FFFFFF"));
        button.setTextColor(Color.BLACK);
        button.setClickable(false); // 일반 모드 시 터치 상위 버블링 우회 강제 유지

        // 편집 모드 실시간 터치 드래그 메커니즘 엔진 바인딩
        button.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private int initLeft, initBottom, initRight, initTop;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // [인터록] 플레이 모드일 때는 해당 리스너를 탈출하여 부모 패드의 호버 엔진으로 바인딩을 이양합니다.
                if (!isEditMode) return false; 

                GameButton btn = (GameButton) v;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) btn.getLayoutParams();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 대상 버튼 포커싱 및 크기 제어 툴바 연결 액션
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

                        // 배치 Gravity 베이스라인 기준에 따른 실시간 상대 좌표 추적 업데이트 알고리즘
                        if ((lp.gravity & Gravity.LEFT) == Gravity.LEFT) {
                            lp.leftMargin = initLeft + (int) diffX;
                        } else if ((lp.gravity & Gravity.RIGHT) == Gravity.RIGHT) {
                            lp.rightMargin = initRight - (int) diffX;
                        }

                        if ((lp.gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                            lp.bottomMargin = initBottom - (int) diffY;
                        } else if ((lp.gravity & Gravity.TOP) == Gravity.TOP) {
                            lp.topMargin = initTop + (int) diffY;
                        }

                        btn.setLayoutParams(lp);
                        break;
                }
                return true; // 편집 작동 중일 때는 상위 뷰로 터치가 증발하는 것을 완전 가로채기 차단
            }
        });

        return button;
    }

    // 드래그 대상 버튼 활성화 하이라이트 및 SizeBar 동기화 제어 함수
    private void executeButtonSelection(GameButton btn) {
        if (selectedButton != null) {
            selectedButton.setAlpha(1.0f);
            selectedButton.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }
        selectedButton = btn;
        selectedButton.setAlpha(0.6f); // 편집 대상 시각화 알림
        
        sizeBar.setVisibility(View.VISIBLE);
        refreshScaleText();
    }

    private void refreshScaleText() {
        if (selectedButton != null && tvScale != null) {
            int ratio = Math.round(selectedButton.scaleFactor * 100);
            tvScale.setText("크기: " + ratio + "%");
        }
    }

    // 마스터 셋 편집 토글 버튼의 클릭 인터록 액션 제어 시스템
    private void setupEditModeInteraction(final Button toggleBtn, final FrameLayout pad) {
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEditMode = !isEditMode;
                if (isEditMode) {
                    toggleBtn.setText("편집 Mode: ON");
                    toggleBtn.setBackgroundColor(Color.parseColor("#CC00AA00")); // 그린 톤 스위칭
                    clearHoverOperationalStates(pad);
                } else {
                    toggleBtn.setText("편집 Mode: OFF");
                    toggleBtn.setBackgroundColor(Color.parseColor("#CCAA0000")); // 레드 톤 복원
                    sizeBar.setVisibility(View.GONE);
                    if (selectedButton != null) {
                        selectedButton.setAlpha(1.0f);
                        selectedButton = null;
                    }
                }
            }
        });
    }

    // 인게임 터치 조작 범용 멀티 터치 호버 엔진 (편집 모드 시 무력화 인터록 내장)
    private void setupIntegratedHoverEngine(final FrameLayout pad) {
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isEditMode) return false; // 편집 작동 시 호버 인풋 스트림 완전 셧다운

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
                            // 확장형 GameButton 인스턴스 타겟만 필터링하여 호버 연산 매칭
                            if (child instanceof GameButton) {
                                if (x >= child.getLeft() && x <= child.getRight() &&
                                    y >= child.getTop() && y <= child.getBottom()) {
                                    nextFrameStates[j] = true;
                                }
                            }
                        }
                    }
                }

                // 프레임 연산 동기화 유도 (KeyDown / KeyUp 실시간 분기 처리)
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
                return true;
            }
        });
    }

    // 모드 전환 시 잔여 키 눌림 입력 락 해제 헬퍼 함수
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

    // 안드로이드 하드웨어 기판 입력 커널 다이렉트 패킷 주입기
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
