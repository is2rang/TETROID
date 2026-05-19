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
               
               // 1. 스타일(CSS) 정의
               "var style = document.createElement('style');" +
               "style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "#controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:99999999 !important; pointer-events:auto !important; touch-action:none; display:block !important; } " +
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

               // 3. [최적화 핵심] 완전 우회용 원시 키 입력 발생기 정의
               "function sendKeyEvent(type, keyCode, keyName) {" +
               // 포커스 강제 리턴 대상 지정: 게임 내부의 canvas 또는 맨 앞 요소 찾기
               "  var gameCanvas = document.querySelector('canvas') || document.activeElement || document.body;" +
               "  if (gameCanvas && typeof gameCanvas.focus === 'function') {" +
               "    gameCanvas.focus();" + // [우회 1] 입력 전에 게임 메인 화면으로 포커스를 강제 이주 시킵니다.
               "  }" +
               
               // [우회 2] 최신 웹 표준 초기화 함수(initKeyboardEvent)를 사용하여 브라우저가 하드웨어 입력으로 오해하도록 모방
               "  var event = new KeyboardEvent(type, {" +
               "    key: keyName," +
               "    code: keyName," +
               "    keyCode: keyCode," +
               "    which: keyCode," +
               "    bubbles: true," +
               "    cancelable: true," +
               "    composed: true," +
               "    view: window" +
               "  });" +
               
               // 객체 변조 방어막 우회용 (TETR.IO 내부 라이브러리 인식 대응)
               "  Object.defineProperty(event, 'keyCode', { get: function() { return keyCode; } });" +
               "  Object.defineProperty(event, 'which', { get: function() { return keyCode; } });" +
               
               // 윈도우, 도큐먼트, 게임 화면 전체에 주입 신호를 동시에 난사합니다.
               "  if (gameCanvas) gameCanvas.dispatchEvent(event);" +
               "  document.dispatchEvent(event);" +
               "  window.dispatchEvent(event);" +
               "}" +

               // 4. 정밀 좌표 히트테스트 기반 터치 감지 시스템
               "var activeButtons = new Set();" +
               
               "function processTouches(e) {" +
               "  e.preventDefault();" +
               "  var currentActive = new Set();" +
               "  var buttons = Array.from(document.querySelectorAll('.pad-btn'));" +
               
               "  var btnRects = buttons.map(function(btn) {" +
               "    return { element: btn, rect: btn.getBoundingClientRect() };" +
               "  });" +
               
               "  for (var i = 0; i < e.touches.length; i++) {" +
               "    var touch = e.touches[i];" +
               "    var tx = touch.clientX;" +
               "    var ty = touch.clientY;" +
               
               "    for (var j = 0; j < btnRects.length; j++) {" +
               "      var b = btnRects[j];" +
               "      if (tx >= b.rect.left && tx <= b.rect.right && ty >= b.rect.top && ty <= b.rect.bottom) {" +
               "        currentActive.add(b.element);" +
               "      }" +
               "    }" +
               "  }" +
               
               // 버튼 진입 처리 (keydown)
               "  currentActive.forEach(function(el) {" +
               "    if (!activeButtons.has(el)) {" +
               "      el.classList.add('active');" +
               "      sendKeyEvent('keydown', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               
               // 버튼 이탈 처리 (keyup)
               "  activeButtons.forEach(function(el) {" +
               "    if (!currentActive.has(el)) {" +
               "      el.classList.remove('active');" +
               "      sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               
               "  activeButtons = currentActive;" +
               "}" +

               // 이벤트 연결
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

               // 6. 감시자 복구 로직 유지
               "var observer = new MutationObserver(function() {" +
               "  if (!document.getElementById('controller-overlay')) {" +
               "    targetBody.appendChild(overlay);" +
               "    bindHoverControls();" +
               "  }" +
               "});" +
               "observer.observe(targetBody, { childList: true, subtree: true });" +
               "})();";
    }
}
