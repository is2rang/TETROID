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

        // 하드웨어 가속 활성화 (UI 버벅임 방지)
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
               // 중복 생성 방지 안전장치
               "if (document.getElementById('tetroid-toggle-btn')) return;" +
               
               // 1. 스타일(CSS) 정의 (토글 버튼 스타일 및 오버레이 판 초기 상태 지정)
               "var style = document.createElement('style');" +
               "style.innerHTML = '*{ -webkit-user-select:none; user-select:none; -webkit-touch-callout:none; } " +
               "#controller-overlay { position:fixed; top:0; left:0; width:100%; height:100%; z-index:99999998 !important; pointer-events:auto !important; touch-action:none; display:block; } " +
               ".pad-btn { position:absolute; width:75px; height:75px; background:rgba(255,255,255,0.2) !important; border:2px solid rgba(255,255,255,0.4) !important; border-radius:50%; color:white !important; font-weight:bold; font-size:18px; display:flex !important; justify-content:center; align-items:center; pointer-events:none; z-index:99999998 !important; transition: background 0.05s; } " +
               ".pad-btn.active { background:rgba(255,255,255,0.7) !important; transform:scale(0.95); } " +
               "#btn-left { bottom:110px; left:40px; } #btn-right { bottom:110px; left:170px; } #btn-soft { bottom:30px; left:105px; } " +
               "#btn-ccw { bottom:110px; right:170px; } #btn-cw { bottom:190px; right:105px; } " +
               "#btn-hard { bottom:110px; right:40px; background:rgba(255,50,50,0.3) !important; border-color:rgba(255,100,100,0.5) !important; } " +
               "#btn-hard.active { background:rgba(255,50,50,0.8) !important; } " +
               "#btn-hold { top:40px; left:40px; width:65px; height:65px; font-size:14px; background:rgba(50,150,255,0.3) !important; } " +
               "#btn-hold.active { background:rgba(50,150,255,0.8) !important; } " +
               // 우측 상단 무적 고정 토글 버튼 디자인 (오버레이 외부 레이어 z-index 최고 등급)
               "#tetroid-toggle-btn { position:fixed; top:15px; right:15px; width:55px; height:35px; background:rgba(0,0,0,0.6) !important; border:1.5px solid rgba(255,255,255,0.7) !important; border-radius:6px; color:rgba(255,255,255,0.9) !important; font-size:11px; font-weight:bold; display:flex; justify-content:center; align-items:center; z-index:99999999 !important; pointer-events:auto !important; cursor:pointer; } " +
               "#tetroid-toggle-btn:active { background:rgba(100,100,100,0.8) !important; }';" +
               "document.head.appendChild(style);" +

               // 2. 가상 패드 오버레이 레이아웃(HTML) 생성
               "var overlay = document.createElement('div');" +
               "overlay.id = 'controller-overlay';" +
               "overlay.innerHTML = `<div id=\"btn-left\" class=\"pad-btn\" data-code=\"37\" data-name=\"ArrowLeft\">◀</div>" +
               "<div id=\"btn-right\" class=\"pad-btn\" data-code=\"39\" data-name=\"ArrowRight\">▶</div>" +
               "<div id=\"btn-soft\" class=\"pad-btn\" data-code=\"40\" data-name=\"ArrowDown\">▼</div>" +
               "<div id=\"btn-ccw\" class=\"pad-btn\" data-code=\"90\" data-name=\"KeyZ\">Z</div>" +
               "<div id=\"btn-cw\" class=\"pad-btn\" data-code=\"88\" data-name=\"KeyX\">X</div>" +
               "<div id=\"btn-hard\" class=\"pad-btn\" data-code=\"32\" data-name=\"Space\">SPACE</div>" +
               "<div id=\"btn-hold\" class=\"pad-btn\" data-code=\"67\" data-name=\"KeyC\">HOLD</div>`;" +

               // 3. UI 토글 온/오프 버튼(HTML) 생성
               "var toggleBtn = document.createElement('div');" +
               "toggleBtn.id = 'tetroid-toggle-btn';" +
               "toggleBtn.innerText = 'PAD ON';" + // 기본값 ON 상태

               // 4. 원래 정상 작동하던 안정적인 KeyboardEvent 인풋 엔진 복구
               "function sendKeyEvent(type, keyCode, keyName) {" +
               "  var target = document.activeElement || document.body || window;" +
               "  var event = new KeyboardEvent(type, { key:keyName, code:keyName, keyCode:keyCode, which:keyCode, bubbles:true, cancelable:true, view:window });" +
               "  target.dispatchEvent(event); window.dispatchEvent(event);" +
               "}" +

               // 5. 호버용 정밀 좌표 추적 시스템 (동작 안정화 유지)
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

               // 오버레이 터치 리스너 연결 함수
               "function bindHoverControls() {" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (overlayEl) {" +
               "    overlayEl.addEventListener('touchstart', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchmove', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchend', processTouches, { passive: false });" +
               "    overlayEl.addEventListener('touchcancel', processTouches, { passive: false });" +
               "  }" +
               "}" +

               // 6. [핵심 기능] 토글 기능 구현 (터치 차단막을 켜고 끄기)
               "toggleBtn.addEventListener('click', function(e) {" +
               "  e.preventDefault();" +
               "  e.stopPropagation();" +
               "  var overlayEl = document.getElementById('controller-overlay');" +
               "  if (overlayEl) {" +
               "    if (overlayEl.style.display === 'none') {" +
               "      overlayEl.style.display = 'block';" + // 가상패드 활성화
               "      toggleBtn.innerText = 'PAD ON';" +
               "      toggleBtn.style.background = 'rgba(0,0,0,0.6)';" +
               "    } else {" +
               "      overlayEl.style.display = 'none';" + // 가상패드 차단막 완전 해제 (TETR.IO 터치 완전 가능)
               "      toggleBtn.innerText = 'PAD OFF';" +
               "      toggleBtn.style.background = 'rgba(255,50,50,0.6)';" +
               "      // 미처 떼어지지 못한 잔여 키 입력 업(keyup) 초기화 발생" +
               "      activeButtons.forEach(function(el) {" +
               "        el.classList.remove('active');" +
               "        sendKeyEvent('keyup', parseInt(el.dataset.code), el.dataset.name);" +
               "      });" +
               "      activeButtons.clear();" +
               "    }" +
               "  }" +
               "}, { passive: false });" +

               // 7. 문서 구조에 안전 안착
               "var targetBody = document.body || document.documentElement;" +
               "targetBody.appendChild(overlay);" +
               "targetBody.appendChild(toggleBtn);" +
               "bindHoverControls();" +

               // 8. MutationObserver 유지 관리 (UI 복구 장치에 토글 호환 구조 반영)
               "var observer = new MutationObserver(function() {" +
               "  if (!document.getElementById('controller-overlay')) {" +
               "    targetBody.appendChild(overlay);" +
               "    bindHoverControls();" +
               "  }" +
               "  if (!document.getElementById('tetroid-toggle-btn')) {" +
               "    targetBody.appendChild(toggleBtn);" +
               "  }" +
               "});" +
               "observer.observe(targetBody, { childList: true, subtree: true });" +
               
               "console.log('TETROID 토글 및 인풋 복구 엔진 안착 완료');" +
               "})();";
    }
}
