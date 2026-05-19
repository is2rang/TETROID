package com.is2rang.tetroid; // 본인의 패키지명에 맞게 유지

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Capacitor WebView 인스턴스 가져오기
        WebView webView = this.getBridge().getWebView();
        
        // 웹뷰 로딩 추적을 위한 클라이언트 설정
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                
                // 테트리오 페이지 로딩 완료 시점에 가상 패드 JS 주입
                if (url.contains("tetr.io")) {
                    // 1번의 스크립트 내용을 압축된 문자열 형태로 실행시킵니다.
                    view.evaluateJavascript(
                        "(function(){" +
                        "if(window.__tetrioMobilePadLoaded)return;window.__tetrioMobilePadLoaded=true;" +
                        "const s=document.createElement('style');s.innerHTML='.mobile-pad-container{position:fixed;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:999999;user-select:none;-webkit-user-select:none;}.v-btn{position:absolute;pointer-events:auto;background:rgba(255,255,255,0.2);border:2px solid rgba(255,255,255,0.4);border-radius:15px;color:white;text-align:center;font-weight:bold;font-size:18px;display:flex;align-items:center;justify-content:center;touch-action:none;}.btn-left{bottom:80px;left:30px;width:70px;height:70px;}.btn-right{bottom:80px;left:190px;width:70px;height:70px;}.btn-soft{bottom:30px;left:110px;width:70px;height:70px;}.btn-hard{bottom:40px;right:30px;width:90px;height:90px;background:rgba(255,0,0,0.3);}.btn-cw{bottom:150px;right:40px;width:70px;height:70px;}.btn-ccw{bottom:130px;right:130px;width:70px;height:70px;}.btn-hold{top:40px;left:20px;width:80px;height:50px;background:rgba(0,255,255,0.2);}';" +
                        "document.head.appendChild(s);" +
                        "const c=document.createElement('div');c.className='mobile-pad-container';document.body.appendChild(c);" +
                        "function k(t,kC,kY,cD){const e=new KeyboardEvent(t,{key:kY,code:cD,keyCode:kC,which:kC,bubbles:true,cancelable:true,composed:true});document.dispatchEvent(e);window.dispatchEvent(e);}" +
                        "function b(txt,cl,m){const btn=document.createElement('div');btn.className='v-btn '+cl;btn.innerText=txt;" +
                        "btn.addEventListener('touchstart',(e)=>{e.preventDefault();k('keydown',m.keyCode,m.key,m.code);},{passive:false});" +
                        "btn.addEventListener('touchend',(e)=>{e.preventDefault();k('keyup',m.keyCode,m.key,m.code);},{passive:false});" +
                        "c.appendChild(btn);}" +
                        "const M={LEFT:{keyCode:37,key:'ArrowLeft',code:'ArrowLeft'},RIGHT:{keyCode:39,key:'ArrowRight',code:'ArrowRight'},SOFT:{keyCode:40,key:'ArrowDown',code:'ArrowDown'},HARD:{keyCode:32,key:' ',code:'Space'},CW:{keyCode:38,key:'ArrowUp',code:'ArrowUp'},CCW:{keyCode:90,key:'z',code:'KeyZ'},HOLD:{keyCode:16,key:'Shift',code:'ShiftLeft'}};" +
                        "b('◀','btn-left',M.LEFT);b('▶','btn-right',M.RIGHT);b('▼','btn-soft',M.SOFT);b('HARD','btn-hard',M.HARD);b('↻','btn-cw',M.CW);b('↺','btn-ccw',M.CCW);b('HOLD','btn-hold',M.HOLD);" +
                        "})();", 
                        null
                    );
                }
            }
        });
    }
}
