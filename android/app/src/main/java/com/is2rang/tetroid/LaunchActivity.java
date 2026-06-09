package com.is2rang.tetroid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class LaunchActivity extends Activity {

    private EditText roomInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 가로 고정
        setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        );

        // 상태바 / 네비게이션바 숨김
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    getWindow().getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        // 검은 배경
        root.setBackgroundColor(Color.BLACK);

        roomInput = new EditText(this);
        roomInput.setHint("Room Code");
        roomInput.setText("");

        roomInput.setTextColor(Color.WHITE);
        roomInput.setHintTextColor(Color.GRAY);

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        dpToPx(300),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        roomInput.setLayoutParams(inputParams);

        Button playBtn = new Button(this);
        playBtn.setText("PLAY");

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        dpToPx(180),
                        dpToPx(60)
                );

        buttonParams.topMargin = dpToPx(20);

        playBtn.setLayoutParams(buttonParams);

        root.addView(roomInput);
        root.addView(playBtn);

        setContentView(root);

        playBtn.setOnClickListener(v -> {

            String roomCode =
                    roomInput.getText()
                            .toString()
                            .trim();

            Intent intent =
                    new Intent(
                            LaunchActivity.this,
                            MainActivity.class
                    );

            intent.putExtra(
                    "roomCode",
                    roomCode
            );

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (roomInput != null) {
            roomInput.setText("");
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources()
                .getDisplayMetrics()
                .density);
    }
}