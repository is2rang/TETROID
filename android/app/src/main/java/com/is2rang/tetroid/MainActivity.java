package com.yourdomain.tetrioplayer; // 본인의 패키지명 유지

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WebView webView = this.getBridge().getWebView();
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                if (url.contains("tetr.io")) {
                    // 실행할 자바스크립트 코드 (SPA 대응 및 안드로이드 WebView 키 버그 수정 버전)
                    String jsCode = "(function() {" +
                        "if (window.__tetrioMobilePadInitialized) return;" +
                        "window.__tetrioMobilePadInitialized = true;" +
                        
                        // 1. 스타일 시트 주입 (중복 방지 id 지정)
                        if (!document.getElementById('tetrio-mobile-style')) {" +
                        "  const s = document.createElement('style');" +
                        "  s.id = 'tetrio-mobile-style';" +
                        "  s.innerHTML = '.mobile-pad-container{position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:9999999;user-select:none;-webkit-user-select:none;}.v-btn{position:absolute;pointer-events:auto;background:rgba(255,255,255,0.2);border:2px solid rgba(255,255,255,0.4);border-radius:15px;color:white;text-align:center;font-weight:bold;font-size:18px;display:flex;align-items:center;justify-content:center;touch-action:none;}.v-btn:active{background:rgba(255,255,255,0.5);}.btn-left{bottom:80px;left:30px;width:70px;height:70px;}.btn-right{bottom:80px;left:190px;width:70px;height:70px;}.btn-soft{bottom:30px;left:110px;width:70px;height:70px;}.btn-hard{bottom:40px;right:30px;width:90px;height:90px;background:rgba(255,0,0,0.3);}.btn-cw{bottom:150px;right:40px;width:70px;height:70px;}.btn-ccw{bottom:130px;right:130px;width:70px;height:70px;}.btn-hold{top:40px;left:20px;width:80px;height:50px;background:rgba(0,255,255,0.2)}';" +
                        "  document.head.appendChild(s);" +
                        "}" +
                        
                        // 2. 키맵 정의
                        "const M = {" +
                        "  LEFT:  {keyCode:37, key:'ArrowLeft',  code:'ArrowLeft'}," +
                        "  RIGHT: {keyCode:39, key:'ArrowRight', code:'ArrowRight'}," +
                        "  SOFT:  {keyCode:40, key:'ArrowDown',  code:'ArrowDown'}," +
                        "  HARD:  {keyCode:32, key:' ',          code:'Space'}," +
                        "  CW:    {keyCode:38, key:'ArrowUp',     code:'ArrowUp'}," +
                        "  CCW:   {keyCode:90, key:'z',          code:'KeyZ'}," +
                        "  HOLD:  {keyCode:16, key:'Shift',      code:'ShiftLeft'}" +
                        "};" +
                        
                        // 3. 안드로이드 WebView 대응 정밀 키 입력 함수 (핵심 수정)
                        "function sendKey(type, m) {" +
                        "  const e = new KeyboardEvent(type, {key: m.key, code: m.code, bubbles: true, cancelable: true, view: window});" +
                        "  Object.defineProperty(e, 'keyCode', {value: m.keyCode, enumerable: true});" +
                        "  Object.defineProperty(e, 'which', {value: m.keyCode, enumerable: true});" +
                        "  Object.defineProperty(e, 'code', {value: m.code, enumerable: true});" +
                        "  window.dispatchEvent(e);" +
                        "  document.dispatchEvent(e);" +
                        "  if(document.activeElement) document.activeElement.dispatchEvent(e);" +
                        "}" +
                        
                        // 4. 버튼 생성 함수
                        "function b(parent, txt, cl, m) {" +
                        "  const btn = document.createElement('div');" +
                        "  btn.className = 'v-btn ' + cl;" +
                        "  btn.innerText = txt;" +
                        "  btn.addEventListener('touchstart', (e) => { e.preventDefault(); sendKey('keydown', m); }, {passive: false});" +
                        "  btn.addEventListener('touchend', (e) => { e.preventDefault(); sendKey('keyup', m); }, {passive: false});" +
                        "  parent.appendChild(btn);" +
                        "}" +
                        
                        // 5. 무한 루프 감시 (0.5초마다 버튼 컨테이너가 씻겨나갔는지 확인 후 재생성)
                        "setInterval(() => {" +
                        "  if (!document.querySelector('.mobile-pad-container') && document.body) {" +
                        "    const c = document.createElement('div');" +
                        "    c.className = 'mobile-pad-container';" +
                        "    b(c, '◀', 'btn-left', M.LEFT);" +
                        "    b(c, '▶', 'btn-right', M.RIGHT);" +
                        "    b(c, '▼', 'btn-soft', M.SOFT);" +
                        "    b(c, 'HARD', 'btn-hard', M.HARD);" +
                        "    b(c, '↻', 'btn-cw', M.CW);" +
                        "    b(c, '↺', 'btn-ccw', M.CCW);" +
                        "    b(c, 'HOLD', 'btn-hold', M.HOLD);" +
                        "    document.body.appendChild(c);" +
                        "  }" +
                        "}, 500);" +
                        
                        "})();";
                    
                    view.evaluateJavascript(jsCode, null);
                }
            }
        });
    }
}
