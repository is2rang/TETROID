(function injectUI() {
    // 버튼 스타일과 레이아웃 생성
    const style = document.createElement('style');
    style.innerHTML = `
        #virtual-gamepad {
            position: fixed; bottom: 20px; left: 0; width: 100%;
            display: flex; justify-content: space-between; padding: 0 20px;
            box-sizing: border-box; z-index: 999999; pointer-events: none;
        }
        .ctrl-btn {
            pointer-events: auto; width: 60px; height: 60px;
            background: rgba(255, 255, 255, 0.2); color: white;
            border: 2px solid rgba(255, 255, 255, 0.4); border-radius: 50%;
        }
    `;
    document.head.appendChild(style);

    const pad = document.createElement('div');
    pad.id = 'virtual-gamepad';
    pad.innerHTML = `
        <div class="d-pad"><button class="ctrl-btn" data-code="ArrowLeft">◀</button></div>
        <div class="action-pad"><button class="ctrl-btn" data-code="Space">DROP</button></div>
    `;
    document.body.appendChild(pad);

    // 이벤트 주입 로직
    const KEY_MAP = { 'ArrowLeft': 37, 'Space': 32 };
    
    document.querySelectorAll('.ctrl-btn').forEach(btn => {
        const code = btn.getAttribute('data-code');
        btn.addEventListener('touchstart', (e) => {
            e.preventDefault();
            const event = new KeyboardEvent('keydown', { code: code, bubbles: true });
            document.dispatchEvent(event);
        }, { passive: false });
    });
})();
