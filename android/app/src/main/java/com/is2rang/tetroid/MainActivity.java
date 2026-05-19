package com.is2rang.tetroid; // 본인의 패키지명에 맞게 수정하세요

import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BridgeActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Capacitor 메인 웹뷰 가져오기 및 최적화
        webView = getBridge().getWebView();
        optimizeWebView(webView);

        // TETR.IO 서버 직접 로드
        webView.loadUrl("https://tetr.io/");

        // 네이티브 키 입력 플러그인 등록
        registerPlugin(TetrioNativeBridge.class);
    }

    private void optimizeWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        
        // 하드웨어 가속 및 렌더링 성능 극대화
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    // --- TETR.IO용 초고속 네이티브 브릿지 플러그인 정의 ---
    @CapacitorPlugin(name = "TetrioNativeBridge")
    public static class TetrioNativeBridge extends Plugin {

        // JavaScript KeyCode와 Android Native KeyEvent 매핑 데이터 맵
        private static final Map<String, Integer> keyMap = new HashMap<>();
        static {
            keyMap.put("ArrowLeft", KeyEvent.KEYCODE_DPAD_LEFT);
            keyMap.put("ArrowRight", KeyEvent.KEYCODE_DPAD_RIGHT);
            keyMap.put("ArrowDown", KeyEvent.KEYCODE_DPAD_DOWN);
            keyMap.put("Space", KeyEvent.KEYCODE_SPACE);
            keyMap.put("KeyZ", KeyEvent.KEYCODE_Z);
            keyMap.put("KeyX", KeyEvent.KEYCODE_X);
            keyMap.put("KeyA", KeyEvent.KEYCODE_A);
            keyMap.put("KeyC", KeyEvent.KEYCODE_C);
        }

        @PluginMethod
        public void sendKey(PluginCall call) {
            String action = call.getString("action"); // "down" 또는 "up"
            String key = call.getString("key");

            if (key == null || action == null || !keyMap.containsKey(key)) {
                call.reject("Invalid arguments");
                return;
            }

            int nativeKeyCode = keyMap.get(key);
            int nativeAction = action.equals("down") ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;

            // 메인 UI 스레드에서 웹뷰에 다이렉트로 물리 키 이벤트 주입
            getActivity().runOnUiThread(() -> {
                WebView mainWebView = getBridge().getWebView();
                long downTime = android.os.SystemClock.uptimeMillis();
                long eventTime = android.os.SystemClock.uptimeMillis();

                KeyEvent keyEvent = new KeyEvent(downTime, eventTime, nativeAction, nativeKeyCode, 0);
                mainWebView.dispatchKeyEvent(keyEvent);
            });

            call.resolve();
        }
    }
}
