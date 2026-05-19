package com.is2rang.tetroid; // 본인의 패키지명으로 반드시 유지하세요!

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Capacitor 메인 웹뷰 엔진 껍데기 가져오기
        WebView webView = getBridge().getWebView();
        
        // 중요: Capacitor 브릿지가 초기화된 후 웹뷰 클라이언트를 가로채어 커스텀 인젝터 장착
        webView.setWebViewClient(new WebViewClient() {
            
            // 페이지 로딩이 시작되거나 화면 렌더링이 유저에게 보이는 첫 순간을 캐치합니다 (onPageFinished보다 확실함)
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                
                // 도메인이 확실하게 테트리오일 때만 호버 스크립트 주입 실행
                if (url.contains("tetr.io")) {
                    injectHoverController(view);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.contains("tetr.io")) {
                    injectHoverController(view);
                }
            }
        });
    }

    /**
     * 자바단에서 웹뷰 내부로 10버튼 호버 엔진 코드를 강제 삽입하는 함수
     */
    private void injectHoverController(WebView view) {
        String javascriptCode = 
            "(function() {" +
            "  if(document.getElementById('touch-overlay')) return;" +
            "  const style = document.createElement('style');" +
            "  style.innerHTML = 'body,html{-webkit-touch-callout:none;-webkit-user-select:none;user-select:none;}#touch-overlay{position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:999999!important;pointer-events:auto;touch-action:none;}.hover-btn{position:absolute;width:75px;height:75px;background:rgba(255,255,255,0.18)!important;border:2px solid rgba(255,255,255,0.4)!important;border-radius:50%;color:#fff!important;font-weight:bold;display:flex;align-items:center;justify-content:center;font-size:16px;pointer-events:none;box-sizing:border-box;transition:background 0.1s,transform 0.1s;}.hover-btn.active{background:rgba(0,255,200,0.5)!important;border-color:rgba(0,255,200,0.8)!important;transform:scale(1.05);}.hover-btn.combo{width:62px;height:62px;background:rgba(255,200,0,0.15)!important;font-size:12px;}.hover-btn.combo.active{background:rgba(255,200,0,0.5)!important;}#btn-left{bottom:90px;left:20px;}#btn-right{bottom:90px;left:160px;}#btn-soft{bottom:20px;left:90px;}#btn-left-soft{bottom:25px;left:25px;}#btn-right-soft{bottom:25px;left:155px;}#btn-ccw{bottom:30px;right:170px;}#btn-cw{bottom:95px;right:110px;}#btn-180{bottom:160px;right:170px;}#btn-hard{bottom:30px;right:30px;}#btn-hold{top:20px;right:20px;width:65px;height:65px;}';" +
            "  document.head.appendChild(style);" +
            "  const overlay = document.createElement('div');" +
            "  overlay.id = 'touch-overlay';" +
            "  overlay.innerHTML = '<div id=\"btn-left\" class=\"hover-btn\" data-key=\"ArrowLeft\">◀</div><div id=\"btn-right\" class=\"hover-btn\" data-key=\"ArrowRight\">▶</div><div id=\"btn-soft\" class=\"hover-btn\" data-key=\"ArrowDown\">▼</div><div id=\"btn-left-soft\" class=\"hover-btn combo\" data-combo=\"ArrowLeft ArrowDown\">◀▼</div><div id=\"btn-right-soft\" class=\"hover-btn combo\" data-combo=\"ArrowRight ArrowDown\">▶▼</div><div id=\"btn-ccw\" class=\"hover-btn\" data-key=\"z\">CCW</div><div id=\"btn-cw\" class=\"hover-btn\" data-key=\"ArrowUp\">CW</div><div id=\"btn-180\" class=\"hover-btn\" data-key=\"a\">180</div><div id=\"btn-hard\" class=\"hover-btn\" data-key=\" \">HARD</div><div id=\"btn-hold\" class=\"hover-btn\" data-key=\"c\">HOLD</div>';" +
            "  document.body.appendChild(overlay);" +
            "  const buttons = [];" +
            "  function cacheLayout() {" +
            "    buttons.length = 0;" +
            "    overlay.querySelectorAll('.hover-btn').forEach(el => {" +
            "      const rect = el.getBoundingClientRect();" +
            "      buttons.push({element:el,left:rect.left,right:rect.right,top:rect.top,bottom:rect.bottom,keys:el.getAttribute('data-key')?[el.getAttribute('data-key')]:el.getAttribute('data-combo').split(' ')});" +
            "    });" +
            "  }" +
            "  window.addEventListener('resize', cacheLayout);" +
            "  cacheLayout(); setTimeout(cacheLayout, 1500);" +
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
            "})();";

        // 네이티브 자바 코드가 웹뷰의 런타임 스레드에 자바스크립트 엔진 코드를 강제 전송 및 실행
        view.post(new Runnable() {
            @Override
            public void run() {
                view.evaluateJavascript(javascriptCode, null);
            }
        });
    }
}
