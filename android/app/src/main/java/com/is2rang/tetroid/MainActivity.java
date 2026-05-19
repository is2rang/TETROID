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
               // 전역 상태 변수로 토글 모드 기억 (window 객체에 안전하게 안착)
               "if (typeof window.isPadEnabled === 'undefined') window.isPadEnabled = true;" +

               // 1. 스타일(CSS) 정의 (가상패드와 토글 버튼 완전 분리 설계)
               "if (!document.getElementById('tetroid-core-style')) {" +
               "  var style = document.createElement('style');" +
               "  style.id = 'tetroid-core-style';" +
               "  style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "  #controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:2147483640 !important; pointer-events:auto !important; touch-action:none; } " +
               "  .pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.2) !important; border:2px solid rgba(255,255,255,0.4) !important; border-radius:50%; color:white !important; font-weight:bold; font-size:18px; display:flex !important; justify-content:center; align-items:center; pointer-events:none; z-index:2147483641 !important; transition: background 0.05s; } " +
               "  .pad-btn.active { background:rgba(255,255,255,0.7) !important; transform:scale(0.95); } " +
               "  #btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "  #btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "  #btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.3) !important; border-color:rgba(255,100,100,0.5) !important; } " +
               "  #btn-hard.active { background:rgba(255,50,50,0.8) !important; } " +
               "  #btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.3) !important; } " +
               "  #btn-hold.active { background:rgba(50,150,255,0.8) !important; } " +
               "  #tetroid-toggle-btn { position:fixed !important; top:15px !important; right:15px !important; width:42px !important; height:42px !important; background:rgba(0,0,0,0.6) !important; border:2px solid rgba(255,255,255,0.8) !important; border-radius:50% !important; color:white !important; font-size:22px !important; display:flex !important; justify-content:center !important; align-items:center !important; z-index:2147483647 !important; pointer-events:auto !important; cursor:pointer !important; box-shadow: 0 4px 8px rgba(0,0,0,0.4); transition: background 0.2s; } " +
               "  #tetroid-toggle-btn:active { background:rgba(100,100,100,0.8) !important; }';" +
               "  document.head.appendChild(style);" +
               "}" +

               // 2. 가상 패드 레이아웃 템플릿 정의
               "function createOverlayElement() {" +
               "  var overlay = document.createElement('div');" +
               "  overlay.id = 'controller-overlay';" +
               "  overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\" data-code=\"37\" data-name=\"ArrowLeft\">◀</div>` +" +
               "                      `<div id=\"btn-right\" class=\"pad-btn\" data-code=\"39\" data-name=\"ArrowRight\">▶</div>` +" +
               "                      `<div id=\"btn-soft\" class=\"pad-btn\" data-code=\"40\" data-name=\"ArrowDown\">▼</div>` +" +
               "                      `<div id=\"btn-ccw\" class=\"pad-btn\" data-code=\"90\" data-name=\"KeyZ\">Z</div>` +" +
               "                      `<div id=\"btn-cw\" class=\"pad-btn\" data-code=\"88\" data-name=\"KeyX\">X</div>` +" +
               "                      `<div id=\"btn-hard\" class=\"pad-btn\" data-code=\"32\" data-name=\"Space\">SPACE</div>` +" +
               "                      `<div id=\"btn-hold\" class=\"pad-btn\" data-code=\"67\" data-name=\"KeyC\">HOLD</div>`;" +
               "  return overlay;" +
               "}" +

               // 초기 렌더링 시 조건부 패드 삽입
               "var root = document.documentElement;" +
               "if (window.isPadEnabled && !document.getElementById('controller-overlay')) {" +
               "  var ov = createOverlayElement();" +
               "  root.appendChild(ov);" +
               "}" +

               // 3. 기호 전용 토글 버튼 생성 (언제나 독립 배치)
               "  var toggleBtn = document.getElementById('tetroid-toggle-btn');" +
               "  if (!toggleBtn) {" +
               "    toggleBtn = document.createElement('div');" +
               "    toggleBtn.id = 'tetroid-toggle-btn';" +
               "    toggleBtn.innerText = window.isPadEnabled ? '●' : '○';" +
               "    document.body.appendChild(toggleBt);" + // documentElement 대신 body에 붙임
               "  }" +

               // 4. KeyboardEvent 기반 입력 발생 함수
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 5. 드래그/문지르기 지원 정밀 터치 호버 트래킹 시스템
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

               // 6. 버튼 온/오프 토글 이벤트 (물리적 DOM 제거 및 변수 제어 방식으로 전면 전개)
               "toggleBtn.onclick = function(e) {" +
               "  e.preventDefault(); e.stopPropagation();" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (window.isPadEnabled) {" +
               "    // PAD OFF 상태로 전환" +
               "    window.isPadEnabled = false;" +
               "    toggleBtn.innerText = '○';" +
               "    toggleBtn.style.borderColor = 'rgba(255,50,50,0.6)';" +
               "    if (overlayEl) {" +
               "      overlayEl.remove();" + // 스타일 숨김 대신 아예 제거하여 감시자 무력화 방지 및 꼬임 차단
               "    }" +
               "    activeButtons.forEach(function(el) {" +
               "      el.classList.remove('active');" +
               "      sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "    });" +
               "    activeButtons.clear();" +
               "  } else {" +
               "    // PAD ON 상태로 전환" +
               "    window.isPadEnabled = true;" +
               "    toggleBtn.innerText = '●';" +
               "    toggleBtn.style.borderColor = 'rgba(255,255,255,0.8)';" +
               "    if (!overlayEl) {" +
               "      var newOv = createOverlayElement();" +
               "      root.appendChild(newOv);" +
               "      bindHoverControls();" +
               "    }" +
               "  }" +
               "};" +

               // 7. 리스너 초기 바인딩
               "bindHoverControls();" +

               // 8. 무적 감시자 (상태변수 window.isPadEnabled가 true일 때만 강제 재생성 유도)
               "  window.tetroidObserver = new MutationObserver(function() {" +
               "    var body = document.body;" +
               "    if (body) {" +
               "      // 패드 재생성" +
               "      if (window.isPadEnabled && !document.getElementById('controller-overlay')) {" +
               "        var ovEl = createOverlayElement();" +
               "        body.appendChild(ovEl);" +
               "        bindHoverControls();" +
               "      }" +
               "      // 토글 버튼 재생성 (이미 있으면 건드리지 않음)" +
               "      if (!document.getElementById('tetroid-toggle-btn')) {" +
               "        body.appendChild(toggleBtn);" +
               "      }" +
               "    }" +
               "  });" +
               "  window.tetroidObserver.observe(document.body, { childList: true, subtree: true });"
    }
}
