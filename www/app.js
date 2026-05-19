(function() {
    // TETR.IO용 키보드 메타데이터 매핑
    const KEY_MAP = {
        'ArrowLeft':  { key: 'ArrowLeft',  keyCode: 37 },
        'ArrowRight': { key: 'ArrowRight', keyCode: 39 },
        'ArrowUp':    { key: 'ArrowUp',    keyCode: 38 },
        'ArrowDown':  { key: 'ArrowDown',  keyCode: 40 },
        'Space':      { key: ' ',          keyCode: 32 },
        'KeyZ':       { key: 'z',          keyCode: 90 },
        'ShiftLeft':  { key: 'Shift',      keyCode: 16 }
    };

    // 정밀 키보드 이벤트 네이티브 전송 함수
    function sendKeyEvent(eventType, codeValue) {
        const meta = KEY_MAP[codeValue] || { key: codeValue, keyCode: 0 };
        
        const event = new KeyboardEvent(eventType, {
            key: meta.key,
            code: codeValue,
            keyCode: meta.keyCode,
            which: meta.keyCode,
            bubbles: true,
            cancelable: true,
            view: window
        });

        window.dispatchEvent(event);
        document.dispatchEvent(event);
    }

    // 초기화 및 터치 이벤트 바인딩
    function initVirtualGamepad() {
        const buttons = document.querySelectorAll('.ctrl-btn');
        
        buttons.forEach(btn => {
            const code = btn.getAttribute('data-code');

            btn.addEventListener('touchstart', (e) => {
                e.preventDefault();
                sendKeyEvent('keydown', code);
            }, { passive: false });

            btn.addEventListener('touchend', (e) => {
                e.preventDefault();
                sendKeyEvent('keyup', code);
            }, { passive: false });
            
            btn.addEventListener('touchcancel', (e) => {
                e.preventDefault();
                sendKeyEvent('keyup', code);
            }, { passive: false });
        });
    }

    // 문서 로드 상태에 따른 안전장치 조치
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initVirtualGamepad);
    } else {
        initVirtualGamepad();
    }
})();
