// inject-script.js
(function() {
  // 중복 실행 방지
  if (document.getElementById('touch-overlay')) return;

  // 1. TETR.IO 화면 위에 얹을 CSS 스타일 생성 및 주입
  const style = document.createElement('style');
  style.innerHTML = `
    body, html { -webkit-touch-callout: none; -webkit-user-select: none; user-select: none; }
    #touch-overlay {
      position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
      z-index: 999999; pointer-events: auto; touch-action: none;
    }
    .hover-btn {
      position: absolute; width: 75px; height: 75px;
      background: rgba(255, 255, 255, 0.15);
      border: 2px solid rgba(255, 255, 255, 0.4);
      border-radius: 50%; color: #fff; font-weight: bold;
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; pointer-events: none; box-sizing: border-box;
      transition: background 0.1s, transform 0.1s;
    }
    .hover-btn.active {
      background: rgba(0, 255, 200, 0.5); border-color: rgba(0, 255, 200, 0.8); transform: scale(1.05);
    }
    .hover-btn.combo { width: 62px; height: 62px; background: rgba(255, 200, 0, 0.15); font-size: 12px; }
    .hover-btn.combo.active { background: rgba(255, 200, 0, 0.5); }

    /* 가로 모드 기준 하단 양손 배치 최적화 좌표 */
    #btn-left        { bottom: 90px; left: 20px; }
    #btn-right       { bottom: 90px; left: 160px; }
    #btn-soft        { bottom: 20px; left: 90px; }
    #btn-left-soft   { bottom: 25px; left: 25px; }
    #btn-right-soft  { bottom: 25px; left: 155px; }
    #btn-ccw         { bottom: 30px; right: 170px; }
    #btn-cw          { bottom: 95px; right: 110px; }
    #btn-180         { bottom: 160px; right: 170px; }
    #btn-hard        { bottom: 30px; right: 30px; }
    #btn-hold        { top: 20px; right: 20px; width: 65px; height: 65px; }
  `;
  document.head.appendChild(style);

  // 2. 가상 패널 및 10개 버튼 DOM 생성
  const overlay = document.createElement('div');
  overlay.id = 'touch-overlay';
  overlay.innerHTML = `
    <div id="btn-left" class="hover-btn" data-key="ArrowLeft">◀</div>
    <div id="btn-right" class="hover-btn" data-key="ArrowRight">▶</div>
    <div id="btn-soft" class="hover-btn" data-key="ArrowDown">▼</div>
    <div id="btn-left-soft" class="hover-btn combo" data-combo="ArrowLeft ArrowDown">◀▼</div>
    <div id="btn-right-soft" class="hover-btn combo" data-combo="ArrowRight ArrowDown">▶▼</div>
    <div id="btn-ccw" class="hover-btn" data-key="z">CCW</div>
    <div id="btn-cw" class="hover-btn" data-key="ArrowUp">CW</div>
    <div id="btn-180" class="hover-btn" data-key="a">180</div>
    <div id="btn-hard" class="hover-btn" data-key=" ">HARD</div>
    <div id="btn-hold" class="hover-btn" data-key="c">HOLD</div>
  `;
  document.body.appendChild(overlay);

  // 3. 호버 엔진 및 최적화 코어 로직
  const buttons = [];
  function cacheLayout() {
    buttons.length = 0;
    overlay.querySelectorAll('.hover-btn').forEach(el => {
      const rect = el.getBoundingClientRect();
      buttons.push({
        element: el, left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom,
        keys: el.getAttribute('data-key') ? [el.getAttribute('data-key')] : el.getAttribute('data-combo').split(' ')
      });
    });
  }
  window.addEventListener('resize', cacheLayout);
  setTimeout(cacheLayout, 500); // DOM 안정화 후 캐싱

  let activeKeys = new Set();
  let lastActiveKeys = new Set();
  const activePointers = new Map();

  function sendKey(type, key) {
    // iframe이 아니므로 현재 window(TETR.IO 본체)에 직접 이벤트 주입 -> CORS 완벽 우회
    const ev = new KeyboardEvent(type, {
      key: key,
      code: key === ' ' ? 'Space' : (key === 'a' ? 'KeyA' : key),
      bubbles: true, cancelable: true, view: window
    });
    window.dispatchEvent(ev);
  }

  function updateInputLoop() {
    activeKeys.clear();

    // 호버 히트 박스 검사
    for (const pos of activePointers.values()) {
      for (let i = 0; i < buttons.length; i++) {
        const btn = buttons[i];
        if (pos.x >= btn.left && pos.x <= btn.right && pos.y >= btn.top && pos.y <= btn.bottom) {
          for (let j = 0; j < btn.keys.length; j++) activeKeys.add(btn.keys[j]);
        }
      }
    }

    // 비주얼 클래스 토글
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      let isHit = false;
      for (const pos of activePointers.values()) {
        if (pos.x >= btn.left && pos.x <= btn.right && pos.y >= btn.top && pos.y <= btn.bottom) {
          isHit = true; break;
        }
      }
      if (isHit) btn.element.classList.add('active');
      else btn.element.classList.remove('active');
    }

    // Keydown / Keyup 이벤트 디스패치
    for (const key of activeKeys) { if (!lastActiveKeys.has(key)) sendKey('keydown', key); }
    for (const key of lastActiveKeys) { if (!activeKeys.has(key)) sendKey('keyup', key); }

    lastActiveKeys = new Set(activeKeys);
    requestAnimationFrame(updateInputLoop);
  }
  requestAnimationFrame(updateInputLoop);

  // 멀티터치 리스너
  overlay.addEventListener('pointerdown', (e) => {
    e.preventDefault();
    activePointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
  });
  overlay.addEventListener('pointermove', (e) => {
    e.preventDefault();
    if (activePointers.has(e.pointerId)) {
      const ptr = activePointers.get(e.pointerId);
      ptr.x = e.clientX; ptr.y = e.clientY;
    }
  });
  const endPtr = (e) => { e.preventDefault(); activePointers.delete(e.pointerId); };
  overlay.addEventListener('pointerup', endPtr);
  overlay.addEventListener('pointercancel', endPtr);
  overlay.addEventListener('pointerleave', endPtr);
})();
