package com.is2rang.tetroid;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebView webView = this.bridge.getWebView();

        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    // 1. 자바스크립트 엔진 및 캐시 설정 최적화
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.getSettings().setDomStorageEnabled(true);

                    // 2. 크롬 클라이언트 장착 (웹 브라우저 내부 이벤트 감지)
                    webView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onProgressChanged(WebView view, int newProgress) {
                            super.onProgressChanged(view, newProgress);
                            // 화면 로딩이 50% 이상 진행될 때마다 선제적으로 패드 주입 시도
                            if (newProgress > 50) {
                                view.evaluateJavascript(getInjectJavascript(), null);
                            }
                        }
                    });

                    // 3. 웹뷰 클라이언트 장착 (주소 이동 및 로딩 완료 감시)
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            // 로딩이 완전히 끝났을 때 확실하게 한 번 더 주입
                            view.evaluateJavascript(getInjectJavascript(), null);
                            
                            // [안전장치] TETR.IO 내부 화면 전환을 대비해 1초 뒤, 3초 뒤에 예약 주입 실행
                            view.postDelayed(new Runnable() {
                                @Override public void run() { view.evaluateJavascript(getInjectJavascript(), null); }
                            }, 1000);
                            view.postDelayed(new Runnable() {
                                @Override public void run() { view.evaluateJavascript(getInjectJavascript(), null); }
                            }, 3000);
                        }
                    });
                }
            });
        }
    }

    // TETR.IO 내부에 강제로 꽂아넣을 자바스크립트 + HTML + CSS 묶음 상자
    private String getInjectJavascript() {
        return "javascript:(function() {" +
               "if (document.getElementById('controller-overlay')) return;" + // 이미 있으면 중복 생성 안 함
               
               // [CSS 주입] 화면 해상도에 상관없이 무조건 최상단(z-index 맥스)에 고정
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

               // [HTML 레이아웃 주입] document.body가 준비 안 되었을 경우 대비 안전장치 포함
               "var targetBody = document.body || document.documentElement;" +
               "var overlay = document.createElement('div');" +
               "overlay.id = 'controller-overlay';" +
               "overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\">◀</div>" +
               "<div id=\"btn-right\" class=\"pad-btn\">▶</div>" +
               "<div id=\"btn-soft\" class=\"pad-btn\">▼</div>" +
               "<div id=\"btn-ccw\" class=\"pad-btn\">Z</div>" +
               "<div id=\"btn-cw\" class=\"pad-btn\">X</div>" +
               "<div id=\"btn-hard\" class=\"pad-btn\">SPACE</div>" +
               "<div id=\"btn-hold\" class=\"pad-btn\">HOLD</div>`;" +
               "targetBody.appendChild(overlay);" +

               // [키 신호 주입]
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // [이벤트 바인딩]
               "var configs = [" +
               "  { id:'btn-left', code:37, name:'ArrowLeft' }," +
               "  { id:'btn-right', code:39, name:'ArrowRight' }," +
               "  { id:'btn-soft', code:40, name:'ArrowDown' }," +
               "  { id:'btn-hard', code:32, name:'Space' }," +
               "  { id:'btn-cw', code:88, name:'KeyX' }," +
               "  { id:'btn-ccw', code:90, name:'KeyZ' }," +
               "  { id:'btn-hold', code:67, name:'KeyC' }" +
               "];" +
               "configs.forEach(function(btn) {" +
               "  var el = document.getElementById(btn.id);" +
               "  if (el) {" +
               "    el.addEventListener('touchstart', function(e) { e.preventDefault(); sendKeyEvent('keydown', btn.code, btn.name); });" +
               "    el.addEventListener('touchend', function(e) { e.preventDefault(); sendKeyEvent('keyup', btn.code, btn.name); });" +
               "  }" +
               "});" +
               "console.log('TETROID 무적 패드 안착 성공!');" +
               "})();";
    }
}
