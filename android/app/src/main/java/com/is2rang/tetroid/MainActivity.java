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

        // 하드웨어 가속 활성화
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

                    webView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onProgressChanged(WebView view, int newProgress) {
                            super.onProgressChanged(view, newProgress);
                            if (newProgress > 60) {
                                view.evaluateJavascript(getInjectJavascript(), null);
                            }
                        }
                    });

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

    private String getInjectJavascript() {
        return "javascript:(function() {" +
               "if (document.getElementById('controller-overlay')) return;" +
               
               // 1. 스타일(CSS) 정의 (호버 시 시각적 피드백을 위해 클래스 분리)
               "var style = document.createElement('style');" +
               "style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "#controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:99999999 !important; pointer-events:auto; touch-action:none; display:block !important; } " +
               ".pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.2) !important; border:2px solid rgba(255,255,255,0.4) !important; border-radius:50%; color:white !important; font-weight:bold; font-size:18px; display:flex !important; justify-content:center; align-items:center; pointer-events:none; z-index:99999999 !important; transition: background 0.05s; } " +
               ".pad-btn.active { background:rgba(255,255,255,0.7) !important; transform:scale(0.95); } " +
               "#btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "#btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "#btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.3) !important; border-color:rgba(255,100,100,0.5) !important; } " +
               "#btn-hard.active { background:rgba(255,50,50,0.8) !important; } " +
               "#btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.3) !important; } " +
               "#btn-hold.active { background:rgba(50,150,255,0.8) !important; }';" +
               "document.head.appendChild(style);" +

               // 2. 패드 레이아웃(HTML) 생성
               "var overlay = document.createElement('div');" +
               "overlay.id = 'controller-overlay';" +
               "overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\" data-code=\"37\" data-name=\"ArrowLeft\">◀</div>" +
               "<div id=\"btn-right\" class=\"pad-btn\" data-code=\"39\" data-name=\"ArrowRight\">▶</div>" +
               "<div id=\"btn-soft\" class=\"pad-btn\" data-code=\"40\" data-name=\"ArrowDown\">▼</div>" +
               "<div id=\"btn-ccw\" class=\"pad-btn\" data-code=\"90\" data-name=\"KeyZ\">Z</div>" +
               "<div id=\"btn-cw\" class=\"pad-btn\" data-code=\"88\" data-name=\"KeyX\">X</div>" +
               "<div id=\"btn-hard\" class=\"pad-btn\" data-code=\"32\" data-name=\"Space\">SPACE</div>" +
               "<div id=\"btn-hold\" class=\"pad-btn\" data-code=\"67\" data-name=\"KeyC\">HOLD</div>`;" +

               // 3. 키 입력 발생 함수
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 4. [핵심] 실시간 터치 추적 및 호버 입력 시스템 구현
               "var activeButtons = new Set();" + // 현재 눌려있는 버튼 보관함
               
               "function processTouches(e) {" +
               "  e.preventDefault();" +
               "  var currentActive = new Set();" +
               
               // 현재 화면을 누르고 있는 모든 손가락 좌표 조사
               "  for (var i = 0; i < e.touches.length; i++) {" +
               "    var touch = e.touches[i];" +
               "    var el = document.elementFromPoint(touch.clientX, touch.clientY);" +
               
               // 손가락 밑에 버튼이 있다면 활성화 목록에 추가
               "    if (el && el.classList.contains('pad-btn')) {" +
               "      currentActive.add(el);" +
               "    }" +
               "  }" +
               
               // [기존에 없었는데 새로 진입한 버튼] -> keydown 발생
               "  currentActive.forEach(function(el) {" +
               "    if (!activeButtons.has(el)) {" +
               "      el.classList.add('active');" +
               "      sendKeyEvent('keydown', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               
               // [누르고 있다가 손가락이 벗어난 버튼] -> keyup 발생
               "  activeButtons.forEach(function(el) {" +
               "    if (!currentActive.has(el)) {" +
               "      el.classList.remove('active');" +
               "      sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               
               "  activeButtons = currentActive;" +
               "}" +

               // 전체 오버레이 영역에 터치 스와이프 이벤트 바인딩
               "function bindHoverControls() {" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (overlayEl) {" +
               "    overlayEl.addEventListener('touchstart', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchmove', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchend', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchcancel', processTouches, { passive: false });" +
               "  }" +
               "}" +

               // 5. 최초 실행 및 구조 안착
               "var targetBody = document.body || document.documentElement;" +
               "targetBody.appendChild(overlay);" +
               "bindHoverControls();" +

               // 6. MutationObserver 안전장치 유지 (UI가 지워져도 복구 후 호버 제어 재연동)
               "var observer = new MutationObserver(function() {" +
               "  if (!document.getElementById('controller-overlay')) {" +
               "    targetBody.appendChild(overlay);" +
               "    bindHoverControls();" +
               "    console.log('TETROID 호버 패드 실시간 복구 완료');" +
               "  }" +
               "});" +
               "observer.observe(targetBody, { childList: true, subtree: true });" +
               
               "console.log('TETROID 무적 호버 입력 엔진 가동');" +
               "})();";
    }
}
