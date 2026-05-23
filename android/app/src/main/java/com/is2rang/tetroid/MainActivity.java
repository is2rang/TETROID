package com.is2rang.tetroid;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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

        // 안드로이드 시스템이 웹뷰 화면 레이아웃을 완전히 준비한 직후(post) 
        // 안전하게 그 위에 네이티브 오버레이 버튼들을 얹도록 대기열에 등록합니다.
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    setupNativeOverlay();
                } catch (Exception e) {
                    Log.e(TAG, "오버레이 레이어 생성 중 크래시 방지 작동: " + e.getMessage(), e);
                }
            }
        });
    }

    // 웹뷰 화면 위에 투명한 네이티브 레이어를 생성하고 버튼들을 배치하는 함수
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
                dpToPx(130) // 버튼 바의 세로 높이: 130dp (모바일 조작 편의를 위해 소폭 확장)
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

        // 5. 게임 플레이에 필요한 가상 키보드 매핑 버튼 4개 생성
        // (텍스트, 키코드, TETR.IO 표준 자바스크립트 키코드 문자열)
        Button btnLeft = createGameButton("◀", 37, "ArrowLeft");
        Button btnRight = createGameButton("▶", 39, "ArrowRight");
        Button btnRotate = createGameButton("↻", 38, "ArrowUp");
        Button btnHardDrop = createGameButton("▼", 32, "Space");

        // 6. 생성된 가상 버튼들을 좌/우 구역 레이아웃에 각각 조립
        leftLayout.addView(btnLeft);
        leftLayout.addView(btnRight);
        rightLayout.addView(btnRotate);
        rightLayout.addView(btnHardDrop);

        // 7. 좌/우 구역을 하단 전체 컨테이너에 결합
        buttonContainer.addView(leftLayout);
        buttonContainer.addView(rightLayout);
        overlayLayout.addView(buttonContainer);

        // 8. Capacitor 웹뷰 위에 안드로이드 네이티브 화면을 완전히 겹쳐서 띄우기 (addContentView)
        addContentView(overlayLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        Log.d(TAG, "네이티브 가상 버튼 레이어가 성공적으로 화면에 부착되었습니다.");
    }

    // 공통 버튼 속성 정의 및 실시간 터치 감지(멀티터치/반응속도 최적화) 헬퍼 함수
    private Button createGameButton(String text, final int keyCode, final String keyName) {
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
        button.setBackgroundColor(Color.parseColor("#77FFFFFF")); // 기본 반투명 흰색 기본값
        button.setTextColor(Color.BLACK);

        // 터치 동작 감지 (모바일 터치 딜레이를 완벽하게 없애기 위해 OnTouchListener 사용)
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getAction();
                
                if (action == MotionEvent.ACTION_DOWN) {
                    // 버튼을 누르는 순간 즉시 색상을 진하게 바꾸고 keydown 이벤트 전송
                    v.setBackgroundColor(Color.parseColor("#BBFFFFFF"));
                    sendKeyEvent(webView, "keydown", keyCode, keyName);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    // 손가락을 떼거나 화면 밖으로 벗어나면 원래 투명도로 돌리고 keyup 이벤트 전송
                    v.setBackgroundColor(Color.parseColor("#77FFFFFF"));
                    sendKeyEvent(webView, "keyup", keyCode, keyName);
                    return true;
                }
                return false;
            }
        });

        return button;
    }

    // 안드로이드 시스템 터치를 웹뷰 내부의 TETR.IO 순수 자바스크립트 키보드 신호로 강제 변환 및 전송하는 함수
    private void sendKeyEvent(final WebView webView, final String action, final int keyCode, final String keyName) {
        final String js = "document.dispatchEvent(new KeyboardEvent('" + action + "', {" +
                "key: '" + keyName + "', " +
                "code: '" + keyName + "', " +
                "keyCode: " + keyCode + ", " +
                "which: " + keyCode + ", " +
                "bubbles: true, " +
                "cancelable: true" +
                "}));";
        
        // 안드로이드 메인 UI 스레드 안전성 보장을 위해 포스트 방식으로 주입 실행
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(js, null);
            }
        });
    }

    // 디바이스의 화면 해상도에 맞추어 dp 단위 크기를 실제 화면 픽셀(Pixel)로 자동 연산해주는 함수
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
