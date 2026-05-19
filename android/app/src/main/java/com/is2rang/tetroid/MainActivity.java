package com.is2rang.tetroid;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 안드로이드 WebView 인스턴스를 가져옵니다. (Capacitor 표준 호환 버전)
        final WebView webView = this.bridge.getWebView();

        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    // 웹뷰에 페이지 로딩 감시자를 붙입니다.
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            
                            // TETR.IO 접속 완료 시 가상 패드를 100% 강제 주입
                            if (url != null && url.contains("tetr.io")) {
                                view.evaluateJavascript(getInjectJavascript(), null);
                            }
                        }
                    });
                }
            });
        }
    }

    // TETR.IO 내부에 강제로 꽂아넣을 자바스크립트 + HTML + CSS 묶음 상자
    private String getInjectJavascript() {
        return "javascript:(function() {" +
               "if (document.getElementById('controller-overlay')) return;" +
               
               // 1. 스타일(CSS) 주입
               "var style = document.createElement('style');" +
               "style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "#controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:999999 !important; pointer-events:none; } " +
               ".pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.25); border:2px solid rgba(255,255,255,0.45); border-radius:50%; color:white; font-weight:bold; font-size:18px; display:flex; justify-content:center; align-items:center; pointer-events:auto; touch-action:none; z-index:999999; } " +
               ".pad-btn:active { background:rgba(255,255,255,0.6); transform:scale(0.92); } " +
               "#btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "#btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "#btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.4); border-color:rgba(255,100,100,0.6); } " +
               "#btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.4); }';" +
               "document.head.appendChild(style);" +

               // 2. 패드 레이아웃(HTML) 주입
               "var overlay = document.createElement('div');" +
               "overlay.id = 'controller-overlay';" +
               "overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\">◀</div>" +
               "<div id=\"btn-right\" class=\"pad-btn\">▶</div>" +
               "<div id=\"btn-soft\" class=\"pad-btn\">▼</div>" +
               "<div id=\"btn-ccw\" class=\"pad-btn\">Z</div>" +
               "<div id=\"btn-cw\" class=\"pad-btn\">X</div>" +
               "<div id=\"btn-hard\" class=\"pad-btn\">SPACE</div>" +
               "<div id=\"btn-hold\" class=\"pad-btn\">HOLD</div>`;" +
               "document.body.appendChild(overlay);" +

               // 3. 키 입력 발생 함수 정의
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 4. 버튼 액션 연동
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
               "console.log('TETROID 네이티브 패드 이식 완료!');" +
               "})();";
    }
}
