package com.is2rang.tetroid; // ⚠️ 본인의 실제 패키지명으로 반드시 수정하세요!

import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Capacitor 기본 웹뷰 가져오기 및 최적화
        webView = getBridge().getWebView();
        optimizeWebView(webView);
        
        // TETR.IO 주소 로드
        webView.loadUrl("https://tetr.io/");

        // 2. 전체 화면을 담을 루트 레이아웃 생성
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 3. 웹뷰를 루트 레이아웃에 첫 번째로 추가 (가장 아래 레이어)
        rootLayout.addView(webView);

        // 4. 가상 패드용 메인 컨테이너 생성 (웹뷰 위에 얹어짐)
        LinearLayout padContainer = new LinearLayout(this);
        padContainer.setOrientation(LinearLayout.HORIZONTAL);
        padContainer.setWeightSum(2f); // 좌/우 영역 비율 분할용
        
        FrameLayout.LayoutParams padParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                550, // 가상 패드 전체 높이 (픽셀 단위, 기기 해상도에 따라 조절 가능)
                Gravity.BOTTOM
        );
        padContainer.setLayoutParams(padParams);
        padContainer.setBackgroundColor(Color.TRANSPAREント); // 배경 투명

        // 5. 왼쪽 조작계 레이아웃 (4열 2행 구조를 위해 세로-가로 조합)
        LinearLayout leftGroup = new LinearLayout(this);
        leftGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        leftGroup.setLayoutParams(groupParams);
        leftGroup.setPadding(20, 10, 20, 10);

        // 왼쪽 첫 번째 행 (L, L+SD, R+SD, R)
        LinearLayout leftRow1 = new LinearLayout(this);
        leftRow1.setOrientation(LinearLayout.HORIZONTAL);
        leftRow1.addView(createKeyButton("L", KeyEvent.KEYCODE_DPAD_LEFT));
        leftRow1.addView(createComboButton("L+SD", KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN));
        leftRow1.addView(createComboButton("R+SD", KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN));
        leftRow1.addView(createKeyButton("R", KeyEvent.KEYCODE_DPAD_RIGHT));
        leftGroup.addView(leftRow1);

        // 왼쪽 두 번째 행 (HOLD, SOFT DROP)
        LinearLayout leftRow2 = new LinearLayout(this);
        leftRow2.setOrientation(LinearLayout.HORIZONTAL);
        leftRow2.addView(createKeyButton("HOLD", KeyEvent.KEYCODE_C));
        leftRow2.addView(createKeyButton("SOFT", KeyEvent.KEYCODE_DPAD_DOWN));
        leftGroup.addView(leftRow2);


        // 6. 오른쪽 조작계 레이아웃 (회전 및 하드드롭)
        LinearLayout rightGroup = new LinearLayout(this);
        rightGroup.setOrientation(LinearLayout.VERTICAL);
        rightGroup.setLayoutParams(groupParams);
        rightGroup.setPadding(20, 10, 20, 10);

        // 오른쪽 첫 번째 행 (CCW, 180, CW)
        LinearLayout rightRow1 = new LinearLayout(this);
        rightRow1.setOrientation(LinearLayout.HORIZONTAL);
        rightRow1.addView(createKeyButton("CCW", KeyEvent.KEYCODE_Z));
        rightRow1.addView(createKeyButton("180", KeyEvent.KEYCODE_A));
        rightRow1.addView(createKeyButton("CW", KeyEvent.KEYCODE_X));
        rightGroup.addView(rightRow1);

        // 오른쪽 두 번째 행 (HARD DROP 단독 대형 버튼)
        LinearLayout rightRow2 = new LinearLayout(this);
        rightRow2.setOrientation(LinearLayout.HORIZONTAL);
        Button btnHd = createKeyButton("HARD DROP", KeyEvent.KEYCODE_SPACE);
        btnHd.setBackgroundColor(Color.parseColor("#440096FF")); // 하드드롭은 강조 색상
        rightRow2.addView(btnHd);
        rightGroup.addView(rightRow2);


        // 7. 메인 컨테이너에 좌/우 그룹 추가 후 루트 레이아웃에 바인딩
        padContainer.addView(leftGroup);
        padContainer.addView(rightGroup);
        rootLayout.addView(padContainer);

        // 최종 화면 적용
        setContentView(rootLayout);
    }

    /**
     * 웹뷰 성능 최적화 세팅
     */
    private void optimizeWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // 하드웨어 가속 강제 활성화 (60fps 보장용)
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    /**
     * 단일 키 입력을 처리하는 네이티브 버튼 생성 헬퍼
     */
    private Button createKeyButton(String text, final int keyCode) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(5, 5, 5, 5);
        button.setLayoutParams(params);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.parseColor("#33FFFFFF")); // 반투명 흰색

        // OnTouchListener를 사용하여 누르고 있을 때와 뗐을 때 즉각 반응 (Click보다 훨씬 빠름)
        button.setOnTouchListener((v, event) -> {
            long now = SystemClock.uptimeMillis();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                v.setBackgroundColor(Color.parseColor("#88FFFFFF")); // 피드백 효과
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
                v.setBackgroundColor(Color.parseColor("#33FFFFFF"));
            }
            return true;
        });

        return button;
    }

    /**
     * 조합 키 입력을 처리하는 네이티브 버튼 생성 헬퍼 (예: Left + Soft Drop)
     */
    private Button createComboButton(String text, final int keyCode1, final int keyCode2) {
        Button button = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(5, 5, 5, 5);
        button.setLayoutParams(params);
        button.setText(text);
        button.setTextColor(Color.YELLOW); // 조합키는 노란색으로 구분
        button.setBackgroundColor(Color.parseColor("#33FFC800"));

        button.setOnTouchListener((v, event) -> {
            long now = SystemClock.uptimeMillis();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode1, 0));
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode2, 0));
                v.setBackgroundColor(Color.parseColor("#88FFC800"));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode1, 0));
                webView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode2, 0));
                v.setBackgroundColor(Color.parseColor("#33FFC800"));
            }
            return true;
        });

        return button;
    }
}
