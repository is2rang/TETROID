// app.js

const gameFrame = document.getElementById('game-frame');
const overlay = document.getElementById('touch-overlay');
let gameWindow = null;

gameFrame.onload = () => {
  try { gameWindow = gameFrame.contentWindow; } catch (e) {}
};

// 성능 최적화: DOM 탐색 횟수를 줄이기 위해 버튼들의 좌표(정적 캐시) 미리 계산
const buttons = [];
function cacheButtonLayout() {
  buttons.length = 0;
  document.querySelectorAll('.hover-btn').forEach(el => {
    const rect = el.getBoundingClientRect();
    buttons.push({
      element: el,
      left: rect.left,
      right: rect.right,
      top: rect.top,
      bottom: rect.bottom,
      // 단일 키 배열 혹은 복합 키 배열로 정규화
      keys: el.getAttribute('data-key') ? [el.getAttribute('data-key')] : el.getAttribute('data-combo').split(' ')
    });
  });
}
// 페이지 로드 및 화면 크기 변경 시마다 좌표 다시 계산
window.addEventListener('load', cacheButtonLayout);
window.addEventListener('resize', cacheButtonLayout);

// 현재 활성화되어 있어야 하는 키들의 집합 (중복 방지용)
let activeKeys = new Set();
// 직전 프레임에서 활성화되었던 키들의 집합
let lastActiveKeys = new Set();
// 현재 화면을 누르고 있는 모든 터치 포인터의 위치 정보 저장
const activePointers = new Map();

// 최적화된 키 이벤트 전송 함수
function sendKey(type, key) {
  const target = gameWindow || window;
  const ev = new KeyboardEvent(type, {
    key: key,
    code: key === ' ' ? 'Space' : (key === 'a' ? 'KeyA' : key),
    bubbles: true,
    cancelable: true,
    view: target
  });
  target.dispatchEvent(ev);
}

// 성능의 핵심: requestAnimationFrame을 이용하여 디스플레이 주사율(60Hz~120Hz)에 맞춰 입력 상태 동기화
function updateInputLoop() {
  activeKeys.clear();

  // 1. 모든 터치 포인터의 좌표를 돌며 어떤 버튼 위에 있는지 판별 (호버 검사)
  for (const pos of activePointers.values()) {
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      if (pos.x >= btn.left && pos.x <= btn.right && pos.y >= btn.top && pos.y <= btn.bottom) {
        // 해당 버튼이 가진 키들을 활성화 집합에 추가
        for (let j = 0; j < btn.keys.length; j++) {
          activeKeys.add(btn.keys[j]);
        }
        // 시각적 활성화 효과 클래스 부여
        if (!btn.element.classList.contains('active')) {
          btn.element.classList.add('active');
        }
      }
    }
  }

  // 시각 효과 해제 처리 (현재 터치되지 않은 버튼들)
  for (let i = 0; i < buttons.length; i++) {
    const btn = buttons[i];
    let isBtnTargeted = false;
    for (const pos of activePointers.values()) {
      if (pos.x >= btn.left && pos.x <= btn.right && pos.y >= btn.top && pos.y <= btn.bottom) {
        isBtnTargeted = true;
        break;
      }
    }
    if (!isBtnTargeted) btn.element.classList.remove('active');
  }

  // 2. 직전 프레임과 비교하여 새로 눌린 키는 keydown, 떨어진 키는 keyup 발생
  // [Keydown 처리]
  for (const key of activeKeys) {
    if (!lastActiveKeys.has(key)) {
      sendKey('keydown', key);
    }
  }
  // [Keyup 처리]
  for (const key of lastActiveKeys) {
    if (!activeKeys.has(key)) {
      sendKey('keyup', key);
    }
  }

  // 상태 스왑 (메모리 할당 없이 데이터 복사)
  lastActiveKeys = new Set(activeKeys);

  requestAnimationFrame(updateInputLoop);
}
// 루프 가동
requestAnimationFrame(updateInputLoop);

// --- 멀티터치 이벤트 리스너 (전체 레이어에서 수신) ---

overlay.addEventListener('pointerdown', (e) => {
  e.preventDefault();
  activePointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
});

overlay.addEventListener('pointermove', (e) => {
  e.preventDefault();
  if (activePointers.has(e.pointerId)) {
    // 손가락이 움직이면(호버) 좌표를 실시간 갱신
    const ptr = activePointers.get(e.pointerId);
    ptr.x = e.clientX;
    ptr.y = e.clientY;
  }
});

const endPointer = (e) => {
  e.preventDefault();
  activePointers.delete(e.pointerId);
};

overlay.addEventListener('pointerup', endPointer);
overlay.addEventListener('pointercancel', endPointer);
overlay.addEventListener('pointerleave', endPointer);
