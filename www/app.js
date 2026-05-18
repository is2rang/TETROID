const iframe = document.getElementById('tetrio');

// 키보드 신호를 만들어 iframe 안으로 쏘아주는 함수
function pressKey(keyCode, keyName, type) {
    if (!iframe || !iframe.contentWindow) return;

    const event = new KeyboardEvent(type, {
        key: keyName,
        code: keyName,
        keyCode: keyCode,
        which: keyCode,
        bubbles: true,
        cancelable: true
    });

    // TETR.IO 화면 내부 윈도우에 키 전달 시도
    iframe.contentWindow.dispatchEvent(event);
}

// 왼쪽 버튼 터치 이벤트
const leftBtn = document.getElementById('move-left');
leftBtn.addEventListener('touchstart', (e) => {
    e.preventDefault(); // 모바일 롱클릭 방지 등
    pressKey(37, 'ArrowLeft', 'keydown');
});
leftBtn.addEventListener('touchend', (e) => {
    e.preventDefault();
    pressKey(37, 'ArrowLeft', 'keyup');
});

// 오른쪽 버튼 터치 이벤트
const rightBtn = document.getElementById('move-right');
rightBtn.addEventListener('touchstart', (e) => {
    e.preventDefault();
    pressKey(39, 'ArrowRight', 'keydown');
});
rightBtn.addEventListener('touchend', (e) => {
    e.preventDefault();
    pressKey(39, 'ArrowRight', 'keyup');
});
