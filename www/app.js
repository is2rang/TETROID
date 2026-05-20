import { Browser } from '@capacitor/browser';

// 버튼으로 TETR.IO 열기
document.getElementById("open-tetrio").addEventListener("click", async () => {
  await Browser.open({ url: "https://tetr.io" });
});

// 커스텀 버튼 이벤트 (추후 WebSocket 연결 필요)
function sendInput(action) {
  console.log("Action:", action);
  // TODO: WebSocket으로 TETR.IO 서버에 전달
}

document.getElementById("btn-left").addEventListener("touchstart", () => {
  sendInput("moveLeft");
});

document.getElementById("btn-right").addEventListener("touchstart", () => {
  sendInput("moveRight");
});

document.getElementById("btn-rotate").addEventListener("touchstart", () => {
  sendInput("rotate");
});

document.getElementById("btn-drop").addEventListener("touchstart", () => {
  sendInput("softDrop");
});
