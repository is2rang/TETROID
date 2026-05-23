package com.is2rang.tetroid;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent; // 네이티브 키 이벤트 임포트
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "TetrioMobile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 안드로이드 시스템 화면 준비가 완료되면 오버레이 레이어를 부착합니다.
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupNativeOverlay();
                } catch (Exception e) {
                    Log.e(TAG, "오버레이 레이어 생성 중 오류 발생: " + e.getMessage(), e);
                }
            }
        });
    }

    // 웹뷰 위에 얹어질 가상 네이티브 버튼 레이아웃 설정
    private void setupNativeOverlay() {
        // 1. 전체 화면을 덮는 투명한 베이스 레이아웃 (FrameLayout)
        FrameLayout overlayLayout = new FrameLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlayLayout.setLayoutParams(overlayParams);

        // 2. 화면 하단에 고정될 버튼 컨테이너 바 (반투명 어두운 배경)
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(130) // 버튼 바의 세로 높이: 130dp
        );
        containerParams.gravity = Gravity.BOTTOM;
        buttonContainer.setLayoutParams(containerParams);
        buttonContainer.setBackgroundColor(Color.parseColor("#55000000")); // 투명도 약 33% 검은색

        // 3. 왼쪽 조작 구역 (좌/우 이동 버튼 배치)
        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        leftLayout.setLayoutParams(leftParams);
        leftLayout.setGravity(Gravity.CENTER);

        // 4. 오른쪽 조작 구역 (회전/하드드롭 버튼 배치)
        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        rightLayout.setLayoutParams(rightParams);
        rightLayout.setGravity(Gravity.CENTER);

        // 5. 안드로이드 하드웨어 키코드(KeyEvent.KEYCODE_*)를 매핑하여 버튼 생성
        // 왼쪽 이동 = 키보드 왼쪽 화살표, 오른쪽 이동 = 오른쪽 화살표
        // 회전 = 위쪽 화살표(TETR.IO 기본값), 하드드롭 = 스페이스바
        Button btnLeft = createGameButton("◀", KeyEvent.KEYCODE_DPAD_LEFT);
        Button btnRight = createGameButton("▶", KeyEvent.KEYCODE_DPAD_RIGHT);
        Button btnRotate = createGameButton("↻", KeyEvent.KEYCODE_DPAD_UP);
        Button btnHardDrop = createGameButton("▼", KeyEvent.KEYCODE_SPACE);

        // 6. 생성된 가상 버튼들을 좌/우 구역 레이아웃에 각각 조립
        leftLayout.addView(btnLeft);
        leftLayout.addView(btnRight);
        rightLayout.addView(btnRotate);
        rightLayout.addView(btnHardDrop);

        // 7. 좌/우 구역을 하단 전체 컨테이너에 결합
        buttonContainer.addView(leftLayout);
        buttonContainer.addView(rightLayout);
        overlayLayout.addView(buttonContainer);

        // 8. Capacitor 웹뷰 위에 안드로이드 네이티브 화면을 완전히 겹쳐서 띄우기
        addContentView(overlayLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        Log.d(TAG, "하드웨어 키 매핑 오버레이가 성공적으로 화면에 부착되었습니다.");
    }

    // 공통 버튼 속성 정의 및 실시간 터치 감지 헬퍼 함수
    private Button createGameButton(String text, final int androidKeyCode) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(75), // 버튼 너비 75dp
                dpToPx(75)  // 버튼 높이 75dp
        );
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        button.setLayoutParams(params);
        button.setText(text);
        button.setTextSize(24);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#77FFFFFF")); // 기본 반투명 흰색
        button.setTextColor(Color.BLACK);

        // 터치 동작 감지 (하드웨어 키 이벤트 연동)
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getAction();
                
                if (action == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#BBFFFFFF")); // 누를 때 피드백 효과
                    // 안드로이드 OS에 실제 물리 키가 눌렸음을 전송 (ACTION_DOWN)
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_DOWN, androidKeyCode);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(Color.parseColor("#77FFFFFF")); // 뗄 때 원상복구
                    // 안드로이드 OS에 물리 키에서 손을 뗐음을 전송 (ACTION_UP)
                    sendNativeKeyEvent(webView, KeyEvent.ACTION_UP, androidKeyCode);
                    return true;
                }
                return false;
            }
        });

        return button;
    }

    // 웹뷰 내부로 안드로이드 순정 하드웨어 키 이벤트를 강제 주입하는 함수 (보안 우회 핵심)
    private void sendNativeKeyEvent(final WebView webView, final int keyAction, final int androidKeyCode) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                // 웹뷰가 현재 포커스를 가지고 있어야 키 입력이 정상 수신됩니다.
                webView.requestFocus();
                
                // 안드로이드가 지원하는 진짜 키 입력 객체를 생성하여 웹뷰 스트림에 주입합니다.
                KeyEvent keyEvent = new KeyEvent(keyAction, androidKeyCode);
                webView.dispatchKeyEvent(keyEvent);
            }
        });
    }

    // 디바이스의 화면 해상도에 맞추어 dp 단위 크기를 실제 화면 픽셀(Pixel)로 자동 연산해주는 함수
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
