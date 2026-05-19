// Capacitor가 로드되지 않은 일반 브라우저 환경 에러 방지
const NativeBridge = window.Capacitor?.Plugins?.TetrioNativeBridge;

function sendKeyEvent(action, key) {
  if (NativeBridge) {
    NativeBridge.sendKey({ action, key });
  } else {
    console.log(`[Native Bridge 미연결] Action: ${action}, Key: ${key}`);
  }
}

// 버튼 ID와 TETR.IO 매핑 테이블
const buttonConfig = {
  'btn-left': { keys: ['ArrowLeft'] },
  'btn-right': { keys: ['ArrowRight'] },
  'btn-sd': { keys: ['ArrowDown'] },
  'btn-hd': { keys: ['Space'] },
  'btn-ccw': { keys: ['KeyZ'] },
  'btn-cw': { keys: ['KeyX'] },
  'btn-180': { keys: ['KeyA'] },
  'btn-hold': { keys: ['KeyC'] },
  // 조합 버튼 처리
  'btn-left-sd': { keys: ['ArrowLeft', 'ArrowDown'] },
  'btn-right-sd': { keys: ['ArrowRight', 'ArrowDown'] }
};

// 이벤트 바인딩 함수 (지연 방지를 위해 passive: false 및 오버헤드 최소화)
Object.keys(buttonConfig).forEach(btnId => {
  const btn = document.getElementById(btnId);
  if (!btn) return;

  const targetKeys = buttonConfig[btnId].keys;

  btn.addEventListener('touchstart', (e) => {
    e.preventDefault();
    targetKeys.forEach(key => sendKeyEvent('down', key));
  }, { passive: false });

  btn.addEventListener('touchend', (e) => {
    e.preventDefault();
    targetKeys.forEach(key => sendKeyEvent('up', key));
  }, { passive: false });

  btn.addEventListener('touchcancel', (e) => {
    e.preventDefault();
    targetKeys.forEach(key => sendKeyEvent('up', key));
  }, { passive: false });
});
