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
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "TetrioMobile";

    // 호버 상태 및 키코드를 유연하게 관리하기 위해 순정 Button을 확장한 커스텀 클래스 정의
    private class GameButton extends androidx.appcompat.widget.AppCompatButton {
        final int androidKeyCode;
        boolean isCurrentPressed = false; // 현재 손가락이 이 버튼 위에 올라와 있는지 여부

        public GameButton(Context context, int androidKeyCode) {
            super(context);
            this.androidKeyCode = androidKeyCode;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // 화면 가로 고정 및 전체 화면 몰입 모드 유지
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
            Log.e(TAG, "시스템 UI 설정 실패: " + e.getMessage());
        }

        // 웹뷰 준비 완료 후 고급 호버 게임패드 주입
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupHoverGamePad();
                } catch (Exception e) {
                    Log.e(TAG, "게임패드 생성 중 에러: " + e.getMessage(), e);
                }
            }
        });
    }

    // 호버/슬라이드가 작동하는 양손형 패드 구성
    private void setupHoverGamePad() {
        FrameLayout rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        FrameLayout overlayLayout = new FrameLayout(this);
        overlayLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // [왼손 패드 컨테이너] (좌, 소프트드롭, 우)
        LinearLayout leftPad = new LinearLayout(this);
        leftPad.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams leftPadParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        leftPadParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        leftPadParams.setMargins(dpToPx(20), 0, 0, dpToPx(20));
        leftPad.setLayoutParams(leftPadParams);

        // [오른손 패드 컨테이너] (홀드, 반시계, 시계, 하드드롭)
        LinearLayout rightPad = new LinearLayout(this);
        rightPad.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams rightPadParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        rightPadParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        rightPadParams.setMargins(0, 0, dpToPx(20), dpToPx(20));
        rightPad.setLayoutParams(rightPadParams);

        // 커스텀 버튼 생성
        GameButton btnLeft = createGameButton("◀", KeyEvent.KEYCODE_DPAD_LEFT, 70);
        GameButton btnSoftDrop = createGameButton("▼", KeyEvent.KEYCODE_DPAD_DOWN, 70);
        GameButton btnRight = createGameButton("▶", KeyEvent.KEYCODE_DPAD_RIGHT, 70);

        GameButton btnHold = createGameButton("H", KeyEvent.KEYCODE_C, 65);
        GameButton btnRotateCCW = createGameButton("↺", KeyEvent.KEYCODE_Z, 65);
        GameButton btnRotateCW = createGameButton("↻", KeyEvent.KEYCODE_DPAD_UP, 65);
        GameButton btnHardDrop = createGameButton("DROP", KeyEvent.KEYCODE_SPACE, 85);

        // 각각의 패드 컨테이너에 버튼 조립
        leftPad.addView(btnLeft);
        leftPad.addView(btnSoftDrop);
        leftPad.addView(btnRight);

        rightPad.addView(btnHold);
        rightPad.addView(btnRotateCCW);
        rightPad.addView(btnRotateCW);
        rightPad.addView(btnHardDrop);

        // [핵심] 개별 버튼이 아닌 패드 레이아웃 전체에 실시간 호버 추적 리스너 작동
        setupPadHoverEngine(leftPad);
        setupPadHoverEngine(rightPad);

        overlayLayout.addView(leftPad);
        overlayLayout.addView(rightPad);
        rootView.addView(overlayLayout);

        Log.d(TAG, "호버/슬라이드 인풋 엔진이 탑재되었습니다.");
    }

    // 기초 버튼 외형 디자인 정의 함수
    private GameButton createGameButton(String text, final int androidKeyCode, int sizeDp) {
        GameButton button = new GameButton(this, androidKeyCode);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(sizeDp),
                dpToPx(75) // 세로 높이 통합 75dp
        );
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        button.setLayoutParams(params);
        button.setText(text);
        button.setTextSize(18);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#66FFFFFF")); // 기본 투명도
        button.setTextColor(Color.BLACK);
        button.setClickable(false); // 개별 터치 가로채기 방지
        return button;
    }

    // 손가락의 위치를 실시간으로 스캔하여 버튼 위를 스치기만 해도 입력값을 주입하는 호버 엔진
    private void setupPadHoverEngine(final LinearLayout pad) {
        pad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getActionMasked();
                int pointerCount = event.getPointerCount();
                int childCount = pad.getChildCount();

                // 이번 실시간 터치 프레임에서 활성화될 버튼 상태 배열
                boolean[] nextFrameStates = new boolean[childCount];

                // 손가락이 화면에 닿아있는 중일 때만 좌표 실시간 추적 연산 진행
                if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
                    for (int i = 0; i < pointerCount; i++) {
                        // 멀티터치 도중 떨어지고 있는 손가락 포인터는 계산에서 실시간 제외
                        if (action == MotionEvent.ACTION_POINTER_UP && i == event.getActionIndex()) {
                            continue;
                        }

                        // 패드 내부 기준의 상대 좌표 추출
                        float x = event.getX(i);
                        float y = event.getY(i);

                        // 호버 검사 알고리즘: 현재 손가락 좌표가 자식 버튼 공간 안에 포함되는지 매칭
                        for (int j = 0; j < childCount; j++) {
                            View child = pad.getChildAt(j);
                            if (x >= child.getLeft() && x <= child.getRight() &&
                                y >= child.getTop() && y <= child.getBottom()) {
                                nextFrameStates[j] = true; // 현재 손가락이 해당 버튼 공간을 점유함
                            }
                        }
                    }
                }

                // 이전 상태 프레임과 비교분석하여 변동 사항이 발생한 버튼만 네이티브 패킷 주입
                for (int j = 0; j < childCount; j++) {
                    GameButton btn = (GameButton) pad.getChildAt(j);
                    boolean isCurrentlyHovered = nextFrameStates[j];

                    if (isCurrentlyHovered && !btn.isCurrentPressed) {
                        // 1. 새롭게 진입한 호버 입력 (Hover In -> KeyDown)
                        btn.isCurrentPressed = true;
                        btn.setBackgroundColor(Color.parseColor("#BBFFFFFF")); // 선명하게 변경
                        sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, btn.androidKeyCode);
                    } else if (!isCurrentlyHovered && btn.isCurrentPressed) {
                        // 2. 미끄러져 나가거나 떼어진 호버 이탈 (Hover Out -> KeyUp)
                        btn.isCurrentPressed = false;
                        btn.setBackgroundColor(Color.parseColor("#66FFFFFF")); // 반투명 복구
                        sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, btn.androidKeyCode);
                    }
                }

                return true; // 해당 패드가 영역 터치 제어 스트림을 독점하도록 허용
            }
        });
    }

    // 가상 하드웨어 키보드 패킷 직접 주입기
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
