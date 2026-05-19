package com.your.app.id; // 본인의 실제 패키지 경로로 반드시 수정하세요!

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = this.bridge.getWebView();
        final WebViewClient originalClient = webView.getWebViewClient();

        // 오리지널 Capacitor 클라이언트를 상속 및 프록시 처리하여 기능 확장
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String urlStr = request.getUrl().toString();

                // 1. TETR.IO 메인 도메인 진입 포인트 포착
                if (urlStr.equals("https://tetr.io/")) {
                    try {
                        URL url = new URL(urlStr);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        
                        // 기존 브라우저 요청 헤더 동기화
                        for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }

                        // TETR.IO 오리지널 소스 가져오기
                        InputStream is = conn.getInputStream();
                        Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
                        String html = s.hasNext() ? s.next() : "";
                        is.close();

                        // 2. 로컬 자산(public 폴더 내부 에셋) 추출
                        String localHtml = readAssetFile("index.html");
                        String localCss = readAssetFile("style.css");
                        String localJs = readAssetFile("app.js");

                        // 3. 소스 코드 결합체 작성
                        StringBuilder injection = new StringBuilder();
                        injection.append("<style>").append(localCss).append("</style>");
                        injection.append(localHtml);
                        injection.append("<script>").append(localJs).append("</script>");

                        // 원본 </body> 결속 직전에 코드 강제 주입
                        html = html.replace("</body>", injection.toString() + "</body>");

                        // 4. 보안 헤더 무력화 및 리턴 패키지 조립
                        Map<String, String> responseHeaders = new HashMap<>();
                        for (Map.Entry<String, java.util.List<String>> header : conn.getHeaderFields().entrySet()) {
                            if (header.getKey() != null) {
                                String key = header.getKey().toLowerCase();
                                // 프레임 제한 및 콘텐츠 보안 차단용 헤더 폐기
                                if (!key.equals("x-frame-options") && !key.equals("content-security-policy")) {
                                    responseHeaders.put(header.getKey(), header.getValue().get(0));
                                }
                            }
                        }

                        InputStream injectedStream = new ByteArrayInputStream(html.getBytes("UTF-8"));
                        return new WebResourceResponse("text/html", "UTF-8", conn.getResponseCode(), "OK", responseHeaders, injectedStream);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 타 도메인 및 하위 스크립트, 혹은 Capacitor 내부 자산은 본래 브릿지로 통과 처리
                return originalClient.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return originalClient.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                originalClient.onPageFinished(view, url);
            }
        });
    }

    // 안드로이드 네이티브 자산(Asset Manager) 스트리밍 유틸리티 함수
    private String readAssetFile(String fileName) {
        try {
            InputStream is = getAssets().open("public/" + fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
