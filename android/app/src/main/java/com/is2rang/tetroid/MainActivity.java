package com.yourdomain.tetriomobile;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
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

        // [초강력 크래시 방지 조치]
        // 앱 실행 즉시 코드가 돌면 Capacitor의 뷰 생성 타이밍과 꼬여서 크래시가 납니다.
        // 메인 UI 스레드에서 안전하게 1초(1000ms) 대기 후 오버레이를 생성하도록 시차를 둡니다.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    setupNativeOverlay();
                } catch (Throwable t) {
                    // Exception을 포함한 모든 시스템 에러(Throwable)를 잡아내어 앱이 절대 죽지 않도록 방어합니다.
                    Log.e(TAG, "오버레이 레이어 생성 중 크래시 방지 기전 작동: " + t.getMessage(), t);
                }
            }
        }, 1000); // 1초 지연
    }

    // 웹뷰 화면 위에 완벽하게 독립된 투명 네이티브 버튼 레이어를 얹는 함수
    private void setupNativeOverlay() {
        // 안드로이드 시스템 최상위 루트 뷰그룹 안전하게 가져오기
        final ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        if (rootView == null) {
            Log.e(TAG, "루트 뷰를 찾을 수 없어 오버레이 생성을 중단합니다.");
            return;
        }

        // 1. 전체 화면을 투명하게 덮는 베이스 프레임 레이아웃 생성
        FrameLayout overlayLayout = new FrameLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlayLayout.setLayoutParams(overlayParams);

        // 2. 하단 조작 바 컨테이너 (반투명 어두운 배경)
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(130) // 가로지르는 조작 바 높이 130dp
        );
        containerParams.gravity = Gravity.BOTTOM;
        buttonContainer.setLayoutParams(containerParams);
        buttonContainer.setBackgroundColor(Color.parseColor("#55000000"));

        // 3. 왼쪽 진영 (좌/우 이동 버튼)
        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        leftLayout.setLayoutParams(leftParams);
        leftLayout.setGravity(Gravity.CENTER);

        // 4. 오른쪽 진영 (회전/하드드롭 버튼)
        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        rightLayout.setLayoutParams(rightParams);
        rightLayout.setGravity(Gravity.CENTER);

        // 5. 물리 하드웨어 키코드와 웹 표준 문자열 키셋을 매핑하여 가상 버튼 컴포넌트들 생성
        Button btnLeft = createGameButton("◀", KeyEvent.KEYCODE_DPAD_LEFT, 37, "ArrowLeft");
        Button btnRight = createGameButton("▶", KeyEvent.KEYCODE_DPAD_RIGHT, 39, "ArrowRight");
        Button btnRotate = createGameButton("↻", KeyEvent.KEYCODE_DPAD_UP, 38, "ArrowUp");
        Button btnHardDrop = createGameButton("▼", KeyEvent.KEYCODE_SPACE, 32, "Space");

        // 6. 생성된 버튼들을 각 레이아웃 계층 구조에 결합
        leftLayout.addView(btnLeft);
        leftLayout.addView(btnRight);
        rightLayout.addView(btnRotate);
        rightLayout.addView(btnHardDrop);

        buttonContainer.addView(leftLayout);
        buttonContainer.addView(rightLayout);
        overlayLayout.addView(buttonContainer);

        // 7. 시스템 크래시를 유발할 수 있는 addContentView 대신, 검증된 안전한 addView 방식으로 결합
        rootView.addView(overlayLayout);
        Log.d(TAG, "네이티브 안전 오버레이 레이어가 정상 부착되었습니다.");
    }

    // 공통 버튼 속성 정의 및 실시간 터치 신호 분석 헬퍼 함수
    private Button createGameButton(String text, final int androidKeyCode, final int webKeyCode, final String webKeyName) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(75),
                dpToPx(75)
        );
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        button.setLayoutParams(params);
        button.setText(text);
        button.setTextSize(24);
        button.setPadding(0, 0, 0, 0);
        button.setBackgroundColor(Color.parseColor("#77FFFFFF")); // 기본 반투명 화이트
        button.setTextColor(Color.BLACK);

        // 터치 동작 캐치 (모바일 인풋 레이턴시 제로 최적화)
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getBridge() == null) return false;
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                int action = event.getAction();
                
                if (action == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#BBFFFFFF")); // 다운 시 시각 피드백
                    // [핵심] 하드웨어 물리 키와 소프트웨어 웹 이벤트를 동시에 주입하여 입력 방어를 무력화합니다.
                    executeDualKeyStroke(webView, KeyEvent.ACTION_DOWN, "keydown", androidKeyCode, webKeyCode, webKeyName);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(Color.parseColor("#77FFFFFF")); // 업 시 원상 복구
                    executeDualKeyStroke(webView, KeyEvent.ACTION_UP, "keyup", androidKeyCode, webKeyCode, webKeyName);
                    return true;
                }
                return false;
            }
        });

        return button;
    }

    // 안드로이드 물리 키보드 스트림 신호와 브라우저 가상 자바스크립트 엔진 신호를 양방향 동시 난사하는 함수
    private void executeDualKeyStroke(final WebView webView, final int nativeAction, final String jsAction, final int nativeCode, final int webCode, final String webName) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                // 1. 웹뷰 제어권 포커싱 강제 획득
                webView.requestFocus();
                
                // 2. 안드로이드 시스템 루트에 진짜 하드웨어 물리 키보드가 입력된 것처럼 이벤트를 꽂아넣습니다.
                KeyEvent nativeKeyEvent = new KeyEvent(nativeAction, nativeCode);
                webView.dispatchKeyEvent(nativeKeyEvent);

                // 3. 동시에 웹뷰 스크립트 컨텍스트 내부에 강력한 가상 자바스크립트 키보드 이벤트를 강제 발송합니다.
                String jsInject = "window.dispatchEvent(new KeyboardEvent('" + jsAction + "', {" +
                        "key: '" + webName + "', " +
                        "code: '" + webName + "', " +
                        "keyCode: " + webCode + ", " +
                        "which: " + webCode + ", " +
                        "bubbles: true, " +
                        "cancelable: true" +
                        "})); " +
                        "document.dispatchEvent(new KeyboardEvent('" + jsAction + "', {" +
                        "key: '" + webName + "', " +
                        "code: '" + webName + "', " +
                        "keyCode: " + webCode + ", " +
                        "which: " + webCode + ", " +
                        "bubbles: true, " +
                        "cancelable: true" +
                        "}));";
                webView.evaluateJavascript(jsInject, null);
            }
        });
    }

    // 디바이스 스크린 해상도 비율(Density) 맞춤형 인치 변환 연산기
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
