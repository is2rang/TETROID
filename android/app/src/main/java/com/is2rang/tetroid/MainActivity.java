package com.is2rang.tetroid; // 본인의 패키지명에 맞게 유지하세요.

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Capacitor 내부 웹뷰 객체 가져오기
        WebView webView = getBridge().getWebView();
        
        // 웹뷰 레이어 위에 커스텀 이벤트를 가로채지 않고 패치하기 위해 웹뷰클라이언트 재정의
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 페이지 로딩이 완료되면 위의 호버 스크립트 자바스크립트를 문자열로 컴파일하여 강제 주입합니다.
                // 팁: 성능을 위해 주입할 JS 코드를 한 줄(minify)로 압축하여 넣는 것이 안전합니다.
                view.evaluateJavascript(
                    "(function() {" +
                    "  if(document.getElementById('touch-overlay')) return;" +
                    "  const style = document.createElement('style');" +
                    "  style.innerHTML = 'body,html{-webkit-touch-callout:none;-webkit-user-select:none;user-select:none;}#touch-overlay{position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:999999;pointer-events:auto;touch-action:none;}.hover-btn{position:absolute;width:75px;height:75px;background:rgba(255,255,255,0.15);border:2px solid rgba(255,255,255,0.4);border-radius:50%;color:#fff;font-weight:bold;display:flex;align-items:center;justify-content:center;font-size:16px;pointer-events:none;box-sizing:border-box;transition:background 0.1s,transform 0.1s;}.hover-btn.active{background:rgba(0,255,200,0.5);border-color:rgba(0,255,200,0.8);transform:scale(1.05);}.hover-btn.combo{width:62px;height:62px;background:rgba(255,200,0,0.15);font-size:12px;}.hover-btn.combo.active{background:rgba(255,200,0,0.5);}#btn-left{bottom:90px;left:20px;}#btn-right{bottom:90px;left:160px;}#btn-soft{bottom:20px;left:90px;}#btn-left-soft{bottom:25px;left:25px;}#btn-right-soft{bottom:25px;left:155px;}#btn-ccw{bottom:30px;right:170px;}#btn-cw{bottom:95px;right:110px;}#btn-180{bottom:160px;right:170px;}#btn-hard{bottom:30px;right:30px;}#btn-hold{top:20px;right:20px;width:65px;height:65px;}';" +
                    "  document.head.appendChild(style);" +
                    "  const overlay = document.createElement('div');" +
                    "  overlay.id = 'touch-overlay';" +
                    "  overlay.innerHTML = '<div id=\"btn-left\" class=\"hover-btn\" data-key=\"ArrowLeft\">◀</div><div id=\"btn-right\" class=\"hover-btn\" data-key=\"ArrowRight\">▶</div><div id=\"btn-soft\" class=\"hover-btn\" data-key=\"ArrowDown\">▼</div><div id=\"btn-left-soft\" class=\"hover-btn combo\" data-combo=\"ArrowLeft ArrowDown\">◀▼</div><div id=\"btn-right-soft\" class=\"hover-btn combo\" data-combo=\"ArrowRight ArrowDown\">▶▼</div><div id=\"btn-ccw\" class=\"hover-btn\" data-key=\"z\">CCW</div><div id=\"btn-cw\" class=\"hover-btn\" data-key=\"ArrowUp\">CW</div><div id=\"btn-180\" class=\"hover-btn\" data-key=\"a\">180</div><div id=\"btn-hard\" class=\"hover-btn\" data-key=\" \">HARD</div><div id=\"btn-hold\" class=\"hover-btn\" data-key=\"c\">HOLD</div>';" +
                    "  document.body.appendChild(overlay);" +
                    "  const buttons = [];" +
                    "  overlay.querySelectorAll('.hover-btn').forEach(el => {" +
                    "    const rect = el.getBoundingClientRect();" +
                    "    buttons.push({element:el,left:rect.left,right:rect.right,top:rect.top,bottom:rect.bottom,keys:el.getAttribute('data-key')?[el.getAttribute('data-key')]:el.getAttribute('data-combo').split(' ')});" +
                    "  });" +
                    "  let activeKeys = new Set(), lastActiveKeys = new Set();" +
                    "  const activePointers = new Map();" +
                    "  function sendKey(type, key) {" +
                    "    window.dispatchEvent(new KeyboardEvent(type, {key:key,code:key===\' \' ? \'Space\':(key===\'a\'?\'KeyA\':key),bubbles:true,cancelable:true,view:window}));" +
                    "  }" +
                    "  function updateInputLoop() {" +
                    "    activeKeys.clear();" +
                    "    for(const pos of activePointers.values()){" +
                    "      for(let i=0;i<buttons.length;i++){" +
                    "        const btn=buttons[i];" +
                    "        if(pos.x>=btn.left&&pos.x<=btn.right&&pos.y>=btn.top&&pos.y<=btn.bottom){" +
                    "          for(let j=0;j<btn.keys.length;j++) activeKeys.add(btn.keys[j]);" +
                    "        }" +
                    "      }" +
                    "    }" +
                    "    for(let i=0;i<buttons.length;i++){" +
                    "      const btn=buttons[i]; let isHit=false;" +
                    "      for(const pos of activePointers.values()){" +
                    "        if(pos.x>=btn.left&&pos.x<=btn.right&&pos.y>=btn.top&&pos.y<=btn.bottom){ isHit=true; break; }" +
                    "      }" +
                    "      if(isHit) btn.element.classList.add('active'); else btn.element.classList.remove('active');" +
                    "    }" +
                    "    for(const key of activeKeys){ if(!lastActiveKeys.has(key)) sendKey('keydown',key); }" +
                    "    for(const key of lastActiveKeys){ if(!activeKeys.has(key)) sendKey('keyup',key); }" +
                    "    lastActiveKeys = new Set(activeKeys);" +
                    "    requestAnimationFrame(updateInputLoop);" +
                    "  }" +
                    "  requestAnimationFrame(updateInputLoop);" +
                    "  overlay.addEventListener('pointerdown',(e)=>{e.preventDefault(); activePointers.set(e.pointerId,{x:e.clientX,y:e.clientY});});" +
                    "  overlay.addEventListener('pointermove',(e)=>{e.preventDefault(); if(activePointers.has(e.pointerId)){const ptr=activePointers.get(e.pointerId); ptr.x=e.clientX; ptr.y=e.clientY;}});" +
                    "  const endPtr=(e)=>{e.preventDefault(); activePointers.delete(e.pointerId);};" +
                    "  overlay.addEventListener('pointerup',endPtr); overlay.addEventListener('pointercancel',endPtr); overlay.addEventListener('pointerleave',endPtr);" +
                    "})();", 
                    null
                );
            }
        });
    }
}
