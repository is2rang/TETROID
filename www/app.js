(function() {
    // 중복 실행 방지
    if (window.__tetrioMobilePadLoaded) return;
    window.__tetrioMobilePadLoaded = true;

    // 1. 스타일 시트 동적 주입 (모바일 최적화 및 터치 지연 방지)
    const style = document.createElement('style');
    style.innerHTML = `
        .mobile-pad-container {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            pointer-events: none; /* 게임 화면 터치 관통 */
            z-index: 999999;
            font-family: sans-serif;
            user-select: none;
            -webkit-user-select: none;
        }
        .v-btn {
            position: absolute;
            pointer-events: auto; /* 버튼만 터치 인식 */
            background: rgba(255, 255, 255, 0.2);
            border: 2px solid rgba(255, 255, 255, 0.4);
            border-radius: 15px;
            color: white;
            text-align: center;
            font-weight: bold;
            font-size: 18px;
            display: flex;
            align-items: center;
            justify-content: center;
            touch-action: none; /* 브라우저 줌, 스크롤 방지 (초중요) */
            active { background: rgba(255, 255, 255, 0.5); }
        }
        /* 레이아웃 구성 (폰 기종에 맞게 조절 가능) */
        .btn-left    { bottom: 80px;  left: 30px;  width: 70px; height: 70px; }
        .btn-right   { bottom: 80px;  left: 190px; width: 70px; height: 70px; }
        .btn-soft    { bottom: 30px;  left: 110px; width: 70px; height: 70px; }
        .btn-hard    { bottom: 40px;  right: 30px;  width: 90px; height: 90px; background: rgba(255, 0, 0, 0.3); }
        .btn-cw      { bottom: 150px; right: 40px;  width: 70px; height: 70px; }
        .btn-ccw     { bottom: 130px; right: 130px; width: 70px; height: 70px; }
        .btn-hold    { top: 40px;     left: 20px;  width: 80px; height: 50px; background: rgba(0, 255, 255, 0.2); }
    `;
    document.head.appendChild(style);

    // 2. 가상 패드 컨테이너 생성
    const container = document.createElement('div');
    container.className = 'mobile-pad-container';
    document.body.appendChild(container);

    // 3. 키보드 이벤트 시뮬레이션 함수
    function sendKeyEvent(type, keyCode, key, code) {
        const event = new KeyboardEvent(type, {
            key: key,
            code: code,
            keyCode: keyCode,
            which: keyCode,
            bubbles: true,
            cancelable: true,
            composed: true
        });
        // TETR.IO 핵심 입력 타겟 또는 윈도우 전체에 전송
        document.dispatchEvent(event);
        window.dispatchEvent(event);
    }

    // 4. 버튼 생성 및 이벤트 바인딩 함수
    function createButton(text, className, keyInfo) {
        const btn = document.createElement('div');
        btn.className = `v-btn ${className}`;
        btn.innerText = text;

        // touchstart -> keydown (지연 없음)
        btn.addEventListener('touchstart', (e) => {
            e.preventDefault();
            sendKeyEvent('keydown', keyInfo.keyCode, keyInfo.key, keyInfo.code);
        }, { passive: false });

        // touchend -> keyup
        btn.addEventListener('touchend', (e) => {
            e.preventDefault();
            sendKeyEvent('keyup', keyInfo.keyCode, keyInfo.key, keyInfo.code);
        }, { passive: false });

        container.appendChild(btn);
    }

    // 5. TETR.IO 기본 키맵 매핑 (기본값 기준)
    const KeyMap = {
        LEFT:  { keyCode: 37, key: 'ArrowLeft',  code: 'ArrowLeft' },
        RIGHT: { keyCode: 39, key: 'ArrowRight', code: 'ArrowRight' },
        SOFT:  { keyCode: 40, key: 'ArrowDown',  code: 'ArrowDown' },
        HARD:  { keyCode: 32, key: ' ',          code: 'Space' },
        CW:    { keyCode: 38, key: 'ArrowUp',     code: 'ArrowUp' }, // 시계방향 회전
        CCW:   { keyCode: 90, key: 'z',          code: 'KeyZ' },    // 반시계방향 회전
        HOLD:  { keyCode: 16, key: 'Shift',      code: 'ShiftLeft' }
    };

    // 6. 패드 배치
    createButton('◀', 'btn-left', KeyMap.LEFT);
    createButton('▶', 'btn-right', KeyMap.RIGHT);
    createButton('▼', 'btn-soft', KeyMap.SOFT);
    createButton('HARD', 'btn-hard', KeyMap.HARD);
    createButton('↻', 'btn-cw', KeyMap.CW);
    createButton('↺', 'btn-ccw', KeyMap.CCW);
    createButton('HOLD', 'btn-hold', KeyMap.HOLD);

    console.log("TETR.IO Custom Mobile Pad Injected successfully.");
})();
