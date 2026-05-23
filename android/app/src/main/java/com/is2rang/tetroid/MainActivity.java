package com.yourdomain.tetriomobile;

import android.graphics.Color;
import android.os.Bundle;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 안드로이드 최상위 루트 뷰 가져오기
        FrameLayout rootView = findViewById(android.R.id.content);

        // 2. 버튼들을 얹을 전체 화면 크기의 투명 오버레이 레이아웃 생성
        FrameLayout overlayLayout = new FrameLayout(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        overlayLayout.setLayoutParams(overlayParams);

        // 3. 하단에 배치할 버튼 컨테이너 (반투명 검은색 바 형태)
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(120) // 바의 높이: 120dp
        );
        containerParams.gravity = Gravity.BOTTOM;
        buttonContainer.setLayoutParams(containerParams);
        buttonContainer.setBackgroundColor(Color.parseColor("#44000000"));

        // 4. 왼쪽 구역 레이아웃 (좌/우 이동 버튼 배치)
        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        leftLayout.setLayoutParams(leftParams);
        leftLayout.setGravity(Gravity.CENTER);

        // 5. 오른쪽 구역 레이아웃 (회전/하드드롭 버튼 배치)
        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        rightLayout.setLayoutParams(rightParams);
        rightLayout.setGravity(Gravity.CENTER);

        // 6. 게임 컨트롤 버튼 정의 (텍스트, 키코드, 자바스크립트 키 이름)
        Button btnLeft = createGameButton("◀", 37, "ArrowLeft");
        Button btnRight = createGameButton("▶", 39, "ArrowRight");
        Button btnRotate = createGameButton("↻", 38, "ArrowUp");
        Button btnHardDrop = createGameButton("▼", 32, "Space");

        // 7. 레이아웃 조립하기
        leftLayout.addView(btnLeft);
        leftLayout.addView(btnRight);
        rightLayout.addView(btnRotate);
        rightLayout.addView(btnHardDrop);

        buttonContainer.addView(leftLayout);
        buttonContainer.addView(rightLayout);
        overlayLayout.addView(buttonContainer);

        // 8. 웹뷰 위에 최종 오버레이 레이어 결합
        rootView.addView(overlayLayout);
    }

    // 버튼을 생성하고 터치 이벤트를 바인딩하는 헬퍼 함수
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
        button.setBackgroundColor(Color.parseColor("#88FFFFFF")); // 기본 반투명 흰색
        button.setTextColor(Color.BLACK);

        // 터치 동작 감지 (눌렀을 때 keydown, 뗐을 때 keyup)
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WebView webView = getBridge().getWebView();
                if (webView == null) return false;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor("#CCFFFFFF")); // 누르면 더 진해짐
                    sendKeyEvent(webView, "keydown", keyCode, keyName);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(Color.parseColor("#88FFFFFF")); // 떼면 원래대로
                    sendKeyEvent(webView, "keyup", keyCode, keyName);
                    return true;
                }
                return false;
            }
        });

        return button;
    }

    // 웹뷰 내부의 TETR.IO 화면으로 순수 웹 키보드 이벤트를 강제 전송하는 함수
    private void sendKeyEvent(final WebView webView, final String action, final int keyCode, final String keyName) {
        final String js = "document.dispatchEvent(new KeyboardEvent('" + action + "', {" +
                "key: '" + keyName + "', " +
                "code: '" + keyName + "', " +
                "keyCode: " + keyCode + ", " +
                "which: " + keyCode + ", " +
                "bubbles: true, " +
                "cancelable: true" +
                "}));";
        
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(js, null);
            }
        });
    }

    // 스마트폰 해상도 대응을 위한 dp단위 -> 픽셀 변환 함수
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
