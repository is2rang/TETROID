// 오버레이 엘리먼트 동적 생성
const overlay = document.createElement('div');
overlay.id = 'custom-overlay';
overlay.innerHTML = `
    <div class="pad-btn" id="btn-left" data-key="ArrowLeft" style="bottom: 50px; left: 30px;">◀</div>
    <div class="pad-btn" id="btn-right" data-key="ArrowRight" style="bottom: 50px; left: 110px;">▶</div>
    <div class="pad-btn" id="btn-drop" data-key="Space" style="bottom: 50px; right: 30px;">DROP</div>
`;
document.body.appendChild(overlay);

// 스타일 주입 (생략 - 위 CSS를 style 태그로 추가)

let currentButton = null;

// 전역 터치 이벤트 리스너 (컨테이너가 아닌 window나 body에 걸어 이탈 방지)
window.addEventListener('touchmove', (e) => {
    e.preventDefault(); // WebView 스크롤/바운스 방지 (매우 중요)
    
    const touch = e.touches[0];
    // 현재 손가락 위치에 있는 엘리먼트 추출
    const target = document.elementFromPoint(touch.clientX, touch.clientY);
    
    // 유효한 커스텀 버튼 위에 있는지 확인
    if (target && target.classList.contains('pad-btn')) {
        if (currentButton !== target) {
            // 기존에 누르고 있던 버튼이 있다면 해제
            if (currentButton) releaseKey(currentButton);
            
            // 새로운 버튼 진입 (Hover Enter)
            currentButton = target;
            pressKey(currentButton);
        }
    } else {
        // 버튼 영역을 벗어남 (Hover Leave)
        if (currentButton) {
            releaseKey(currentButton);
            currentButton = null;
        }
    }
}, { passive: false });

window.addEventListener('touchend', () => {
    if (currentButton) {
        releaseKey(currentButton);
        currentButton = null;
    }
});
