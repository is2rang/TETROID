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
                            // 페이지가 로딩되는 중간(40% 이상)부터 끊임없이 주입 시도
                            if (newProgress > 40) {
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
               // 1. 중복 생성 방지용 CSS 클래스 체크 (스타일은 한 번만 추가)
               "if (!document.getElementById('tetroid-core-style')) {" +
               "  var style = document.createElement('style');" +
               "  style.id = 'tetroid-core-style';" +
               "  style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "  #controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:99999995 !important; pointer-events:auto !important; touch-action:none; display:block; } " +
               "  .pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.2) !important; border:2px solid rgba(255,255,255,0.4) !important; border-radius:50%; color:white !important; font-weight:bold; font-size:18px; display:flex !important; justify-content:center; align-items:center; pointer-events:none; z-index:99999996 !important; transition: background 0.05s; } " +
               "  .pad-btn.active { background:rgba(255,255,255,0.7) !important; transform:scale(0.95); } " +
               "  #btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "  #btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "  #btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.3) !important; border-color:rgba(255,100,100,0.5) !important; } " +
               "  #btn-hard.active { background:rgba(255,50,50,0.8) !important; } " +
               "  #btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.3) !important; } " +
               "  #btn-hold.active { background:rgba(50,150,255,0.8) !important; }; " +
               "  #tetroid-toggle-btn { position:fixed; top:15px; right:15px; height:35px; padding: 0 12px; background:rgba(0,0,0,0.75) !important; border:1.5px solid rgba(255,255,255,0.8) !important; border-radius:20px; color:white !important; font-size:12px; font-weight:bold; display:flex; justify-content:center; align-items:center; z-index:99999999 !important; pointer-events:auto !important; cursor:pointer; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }';" +
               "  document.head.appendChild(style);" +
               "}" +

               // 2. 가상 패드 오버레이(HTML) 정의 및 생성
               "var overlay = document.getElementById('controller-overlay');" +
               "if (!overlay) {" +
               "  overlay = document.createElement('div');" +
               "  overlay.id = 'controller-overlay';" +
               "  overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\" data-code=\"37\" data-name=\"ArrowLeft\">◀</div>` +" +
               "                      `<div id=\"btn-right\" class=\"pad-btn\" data-code=\"39\" data-name=\"ArrowRight\">▶</div>` +" +
               "                      `<div id=\"btn-soft\" class=\"pad-btn\" data-code=\"40\" data-name=\"ArrowDown\">▼</div>` +" +
               "                      `<div id=\"btn-ccw\" class=\"pad-btn\" data-code=\"90\" data-name=\"KeyZ\">Z</div>` +" +
               "                      `<div id=\"btn-cw\" class=\"pad-btn\" data-code=\"88\" data-name=\"KeyX\">X</div>` +" +
               "                      `<div id=\"btn-hard\" class=\"pad-btn\" data-code=\"32\" data-name=\"Space\">SPACE</div>` +" +
               "                      `<div id=\"btn-hold\" class=\"pad-btn\" data-code=\"67\" data-name=\"KeyC\">HOLD</div>`;" +
               "}" +

               // 3. 토글 버튼(HTML) 정의 및 생성 (● 기호 반영)
               "var toggleBtn = document.getElementById('tetroid-toggle-btn');" +
               "if (!toggleBtn) {" +
               "  toggleBtn = document.createElement('div');" +
               "  toggleBtn.id = 'tetroid-toggle-btn';" +
               "  toggleBtn.innerText = '● ON';" + // 기본 상태 ● ON
               "}" +

               // 4. 인풋 시스템 (KeyboardEvent)
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 5. 호버 터치 메커니즘
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
               "    var tx = touch.clientX; var ty = touch.clientY;" +
               "    for (var j = 0; j < btnRects.length; j++) {" +
               "      var b = btnRects[j];" +
               "      if (tx >= b.rect.left && tx <= b.rect.right && ty >= b.rect.top && ty <= b.rect.bottom) {" +
               "        currentActive.add(b.element);" +
               "      }" +
               "    }" +
               "  }" +
               "  currentActive.forEach(function(el) {" +
               "    if (!activeButtons.has(el)) {" +
               "      el.classList.add('active');" +
               "      sendKeyEvent('keydown', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               "  activeButtons.forEach(function(el) {" +
               "    if (!currentActive.has(el)) {" +
               "      el.classList.remove('active');" +
               "      sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "    }" +
               "  });" +
               "  activeButtons = currentActive;" +
               "}" +

               "function bindHoverControls() {" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (overlayEl) {" +
               "    overlayEl.removeEventListener('touchstart', processTouches);" +
               "    overlayEl.removeEventListener('touchmove', processTouches);" +
               "    overlayEl.removeEventListener('touchend', processTouches);" +
               "    overlayEl.removeEventListener('touchcancel', processTouches);" +
               "    overlayEl.addEventListener('touchstart', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchmove', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchend', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchcancel', processTouches, { passive: false });" +
               "  }" +
               "}" +

               // 6. 토글 이벤트 바인딩 (● 와 ○ 스위칭)
               "toggleBtn.onclick = function(e) {" +
               "  e.preventDefault(); e.stopPropagation();" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (overlayEl) {" +
               "    if (overlayEl.style.display === 'none') {" +
               "      overlayEl.style.display = 'block';" +
               "      toggleBtn.innerText = '● ON';" +
               "      toggleBtn.style.borderColor = 'rgba(255,255,255,0.8)';" +
               "    } else {" +
               "      overlayEl.style.display = 'none';" +
               "      toggleBtn.innerText = '○ OFF';" +
               "      toggleBtn.style.borderColor = 'rgba(255,50,50,0.6)';" +
               "      activeButtons.forEach(function(el) {" +
               "        el.classList.remove('active');" +
               "        sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "      });" +
               "      activeButtons.clear();" +
               "    }" +
               "  }" +
               "};" +

               // 7. 초기 강제 삽입
               "var targetBody = document.body || document.documentElement;" +
               "if (targetBody) {" +
               "  if (!document.getElementById('controller-overlay')) targetBody.appendChild(overlay);" +
               "  if (!document.getElementById('tetroid-toggle-btn')) targetBody.appendChild(toggleBtn);" +
               "  bindHoverControls();" +
               "}" +

               // 8. [강화된 무적 감시자] 게임 엔진의 화면 덮어쓰기를 0.1초 만에 초기화 방어
               "if (!window.tetroidObserver) {" +
               "  window.tetroidObserver = new MutationObserver(function() {" +
               "    var body = document.body || document.documentElement;" +
               "    if (body) {" +
               "      if (!document.getElementById('controller-overlay')) {" +
               "        body.appendChild(overlay);" +
               "        bindHoverControls();" +
               "      }" +
               "      if (!document.getElementById('tetroid-toggle-btn')) {" +
               "        body.appendChild(toggleBtn);" +
               "      }" +
               "    }" +
               "  });" +
               "  window.tetroidObserver.observe(targetBody, { childList: true, subtree: true });" +
               "}" +
               "})();";
    }
}
