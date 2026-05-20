package com.yourname.tetriomobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var buttonInjector: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 웹페이지에 주기적으로 가상 패드 스크립트를 주입하는 타이머 설정
        buttonInjector = Runnable {
            val webView = this.bridge.webView
            if (webView != null) {
                // 웹뷰 내부에서 실행될 JavaScript 코드 (UI 생성 및 키보드 이벤트 시뮬레이션)
                val jsCode = """
                    (function() {
                        // 이미 버튼이 만들어져 있다면 중복 생성 방지
                        if (document.getElementById('tetrio-custom-pads')) return;

                        // 전체 버튼을 담을 투명 컨테이너 생성
                        var padContainer = document.createElement('div');
                        padContainer.id = 'tetrio-custom-pads';
                        padContainer.style.position = 'fixed';
                        padContainer.style.bottom = '30px';
                        padContainer.style.left = '0';
                        padContainer.style.width = '100%';
                        padContainer.style.zIndex = '999999';
                        padContainer.style.display = 'flex';
                        padContainer.style.justifyContent = 'space-between';
                        padContainer.style.pointerEvents = 'none'; // 버튼 외 빈 공간은 터치 통과

                        // 가상 버튼 생성 헬퍼 함수
                        function createKeyBtn(label, keyCode) {
                            var btn = document.createElement('button');
                            btn.innerText = label;
                            btn.style.width = '65px';
                            btn.style.height = '65px';
                            btn.style.margin = '8px';
                            btn.style.backgroundColor = 'rgba(255, 255, 255, 0.4)';
                            btn.style.color = '#fff';
                            btn.style.border = '2px solid rgba(255,255,255,0.7)';
                            btn.style.borderRadius = '50%';
                            btn.style.fontWeight = 'bold';
                            btn.style.fontSize = '18px';
                            btn.style.pointerEvents = 'auto'; // 버튼 영역은 터치 차단(클릭 가능)

                            // 터치 시작 시 키보드가 눌린 효과 (keydown)
                            btn.addEventListener('touchstart', function(e) {
                                e.preventDefault();
                                window.dispatchEvent(new KeyboardEvent('keydown', { code: keyCode, key: keyCode, bubbles: true }));
                            });
                            // 터치 종료 시 키보드를 뗀 효과 (keyup)
                            btn.addEventListener('touchend', function(e) {
                                e.preventDefault();
                                window.dispatchEvent(new KeyboardEvent('keyup', { code: keyCode, key: keyCode, bubbles: true }));
                            });
                            return btn;
                        }

                        // 왼쪽 영역 (방향 조작: ◀, ▼, ▶)
                        var leftBox = document.createElement('div');
                        leftBox.style.display = 'flex';
                        leftBox.appendChild(createKeyBtn('◀', 'ArrowLeft'));
                        leftBox.appendChild(createKeyBtn('▼', 'ArrowDown'));
                        leftBox.appendChild(createKeyBtn('▶', 'ArrowRight'));

                        // 오른쪽 영역 (회전 및 하드드롭: Z, X, Shift, Space)
                        var rightBox = document.createElement('div');
                        rightBox.style.display = 'flex';
                        rightBox.appendChild(createKeyBtn('Z', 'KeyZ'));       // 반시계 회전
                        rightBox.appendChild(createKeyBtn('X', 'KeyX'));       // 시계 회전
                        rightBox.appendChild(createKeyBtn('H', 'ShiftLeft'));  // 홀드(Hold)
                        rightBox.appendChild(createKeyBtn('⛃', 'Space'));      // 하드 드롭

                        padContainer.appendChild(leftBox);
                        padContainer.appendChild(rightBox);
                        document.body.appendChild(padContainer);
                    })();
                """.trimIndent()
                
                webView.evaluateJavascript(jsCode, null)
            }
            // 2초마다 코드를 반복 실행하여 UI 유지를 확인합니다.
            handler.postDelayed(buttonInjector, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(buttonInjector) // 앱 화면으로 돌아오면 타이머 시작
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(buttonInjector) // 앱이 백그라운드로 가면 타이머 정지
    }
}
