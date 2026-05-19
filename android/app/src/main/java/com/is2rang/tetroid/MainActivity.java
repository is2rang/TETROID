package com.is2rang.tetroid; // 본인의 패키지명으로 유지하세요

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Capacitor 내부 엔진이 스스로 초기화되도록 비워둡니다.
    }
}
