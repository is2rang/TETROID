package com.is2rang.tetroid; // 본인의 패키지명으로 반드시 유지하세요!

import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;
import java.io.InputStream;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        final WebView webView = getBridge().getWebView();
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (url.contains("tetr.io")) {
                    injectScriptFile(view);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.contains("tetr.io")) {
                    injectScriptFile(view);
                }
            }
        });
    }

    /**
     * assets/public/app.js 파일을 읽어와 웹뷰에 안전하게 주입하는 함수
     */
    private void injectScriptFile(WebView view) {
        try {
            // Capacitor의 웹 에셋 폴더에서 app.js를 안전하게 읽어옵니다.
            InputStream is = getAssets().open("public/app.js");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            // 문자열 깨짐 및 따옴표 충돌을 방지하기 위해 Base64 안전 인코딩 방식으로 주입합니다.
            String encodedJS = Base64.encodeToString(buffer, Base64.NO_WRAP);
            
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.evaluateJavascript(
                        "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'text/javascript';" +
                        "script.innerHTML = window.atob('" + encodedJS + "');" +
                        "parent.appendChild(script);" +
                        "})()", null
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}