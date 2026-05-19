package com.is2rang.tetroid;

import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [최적화] 안드로이드 그래픽 하드웨어 가속 강제 활성화 (화면 버벅임 감소)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, 
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        final WebView webView = this.bridge.getWebView();

        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.getSettings().setDomStorageEnabled(true);

                    // 크롬 클라이언트 (로딩 중 선제적 주입)
                    webView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onProgressChanged(WebView view, int newProgress) {
                            super.onProgressChanged(view, newProgress);
                            if (newProgress > 60) {
                                view.evaluateJavascript(getInjectJavascript(), null);
                            }
                        }
                    });

                    // 웹뷰 클라이언트 (로딩 완료 시 주입)
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            view.evaluateJavascript(getInjectJavascript(), null);
                        }
                    });
                }
            });
        }
    }

    // TETR.IO 내부에 강제로 꽂아넣을 실시간 감시형 자바스크립트 엔진
    private String getInjectJavascript() {
        return "javascript:(function() {" +
               "if (document.getElementById('controller-overlay')) return;" +
               
               // 1. 스타일(CSS) 정의
               "var style = document.createElement('style');" +
               "style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "#controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:99999999 !important; pointer-events:none; display:block !important; } " +
               ".pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.3) !important; border:2px solid rgba(255,255,255,0.6) !important; border-radius:50%; color:white !important; font-weight:bold; font-size:18px; display:flex !important; justify-content:center; align-items:center; pointer-events:auto; touch-action:none; z-index:99999999 !important; } " +
               ".pad-btn:active { background:rgba(255,255,255,0.7) !important; transform:scale(0.92); } " +
               "#btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "#btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "#btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.5) !important; border-color:rgba(255,100,100,0.7) !important; } " +
               "#btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.5) !important; }';" +
               "document.head.appendChild(style);" +

               // 2. 패드 레이아웃(HTML) 생성
               "var overlay = document.createElement('div');" +
               "overlay.id = 'controller-overlay';" +
               "overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\">◀</div>" +
               "<div id=\"btn-right\" class=\"pad-btn\">▶</div>" +
               "<div id=\"btn-soft\" class=\"pad-btn\">▼</div>" +
               "<div id=\"btn-ccw\" class=\"pad-btn\">Z</div>" +
               "<div id=\"btn-cw\" class=\"pad-btn\">X</div>" +
               "<div id=\"btn-hard\" class=\"pad-btn\">SPACE</div>" +
               "<div id=\"btn-hold\" class=\"pad-btn\">HOLD</div>`;" +

               // 3. 키 입력 발생 함수
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 4. 이벤트 바인딩 함수 (Pointer 이벤트로 딜레이 제로)
               "function bindButtons() {" +
               "  var configs = [" +
               "    { id:'btn-left', code:37, name:'ArrowLeft' }," +
               "    { id:'btn-right', code:39, name:'ArrowRight' }," +
               "    { id:'btn-soft', code:40, name:'ArrowDown' }," +
               "    { id:'btn-hard', code:32, name:'Space' }," +
               "    { id:'btn-cw', code:88, name:'KeyX' }," +
               "    { id:'btn-ccw', code:90, name:'KeyZ' }," +
               "    { id:'btn-hold', code:67, name:'KeyC' }" +
               "  ];" +
               "  configs.forEach(function(btn) {" +
               "    var el = document.getElementById(btn.id);" +
               "    if (el) {" +
               "      el.addEventListener('pointerdown', function(e) { e.preventDefault(); sendKeyEvent('keydown', btn.code, btn.name); });" +
               "      el.addEventListener('pointerup', function(e) { e.preventDefault(); sendKeyEvent('keyup', btn.code, btn.name); });" +
               "      el.addEventListener('pointerleave', function(e) { e.preventDefault(); sendKeyEvent('keyup', btn.code, btn.name); });" +
               "    }" +
               "  });" +
               "}" +

               // 5. 최초 주입 실행
               "var targetBody = document.body || document.documentElement;" +
               "targetBody.appendChild(overlay);" +
               "bindButtons();" +

               // 6. [최적화 핵심] MutationObserver 돔 변경 상시 감시자 가동
               // TETR.IO가 화면을 새로 갈아엎어서 버튼이 증발하면 실시간으로 감지해 복구합니다.
               "var observer = new MutationObserver(function() {" +
               "  if (!document.getElementById('controller-overlay')) {" +
               "    targetBody.appendChild(overlay);" +
               "    bindButtons();" + // 다시 그려졌으므로 이벤트 리스너 재연동
               "    console.log('TETROID 패드 무단 삭제 감지 -> 실시간 복구 완료!');" +
               "  }" +
               "});" +
               "observer.observe(targetBody, { childList: true, subtree: true });" +
               
               "console.log('TETROID MutationObserver 모니터링 가동 시작');" +
               "})();";
    }
}
