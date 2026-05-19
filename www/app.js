// 1. Capacitor 내장 네이티브 HTTP 플러그인 가져오기
const { CapacitorHttp } = window.Capacitor?.Plugins || {};

const iframe = document.getElementById('game-frame');

// TETR.IO 소스 가져와서 iframe에 주입하는 함수
async function loadTetrio() {
    if (!CapacitorHttp) {
        console.error("Capacitor 환경이 아닙니다. 모바일 기기나 에뮬레이터에서 실행하세요.");
        return;
    }

    try {
        // CORS를 우회하여 TETR.IO 메인 HTML 가져오기
        const response = await CapacitorHttp.get({ url: 'https://tetr.io/' });
        let htmlContent = response.data;

        // 상대 경로 자원들을 실제 TETR.IO 서버로 라우팅하기 위해 <base> 태그 주입
        const baseTag = '<base href="https://tetr.io/">';
        htmlContent = htmlContent.replace('<head>', `<head>${baseTag}`);

        // X-Frame-Options를 우회하기 위해 srcdoc에 직접 주입
        iframe.srcdoc = htmlContent;

        // iframe 내부에 HTML이 완전히 로드되면 키보드 이벤트 바인딩
        iframe.onload = () => {
            initVirtualGamepad();
        };

    } catch (error) {
        console.error('TETR.IO 로딩 실패:', error);
    }
}

// 가상 키보드 이벤트 주입 로직
const KEY_MAP = {
    'ArrowLeft':  { key: 'ArrowLeft',  keyCode: 37 },
    'ArrowRight': { key: 'ArrowRight', keyCode: 39 },
    'ArrowUp':    { key: 'ArrowUp',    keyCode: 38 },
    'ArrowDown':  { key: 'ArrowDown',  keyCode: 40 },
    'Space':      { key: ' ',          keyCode: 32 },
    'KeyZ':       { key: 'z',          keyCode: 90 },
    'ShiftLeft':  { key: 'Shift',      keyCode: 16 }
};

function triggerKey(type, codeValue) {
    if (!iframe.contentWindow) return;

    const meta = KEY_MAP[codeValue] || { key: codeValue, keyCode: 0 };
    
    // srcdoc 구조 덕분에 동일 오리진으로 취급되어 내부 window에 직접 이벤트 주입 가능
    const event = new iframe.contentWindow.KeyboardEvent(type, {
        key: meta.key,
        code: codeValue,
        keyCode: meta.keyCode,
        which: meta.keyCode,
        bubbles: true,
        cancelable: true,
        view: iframe.contentWindow
    });
    
    iframe.contentWindow.dispatchEvent(event);
    iframe.contentWindow.document.dispatchEvent(event);
}

// 버튼 리스너 바인딩
function initVirtualGamepad() {
    document.querySelectorAll('.btn').forEach(btn => {
        const code = btn.getAttribute('data-code');
        
        btn.addEventListener('touchstart', (e) => {
            e.preventDefault();
            triggerKey('keydown', code);
        }, { passive: false });
        
        btn.addEventListener('touchend', (e) => {
            e.preventDefault();
            triggerKey('keyup', code);
        }, { passive: false });

        btn.addEventListener('touchcancel', (e) => {
            e.preventDefault();
            triggerKey('keyup', code);
        }, { passive: false });
    });
}

// 앱 시작 시 TETR.IO 원격 로드 실행
window.addEventListener('DOMContentLoaded', loadTetrio);
