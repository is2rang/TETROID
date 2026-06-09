package com.is2rang.tetroid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        } catch (Exception e) {
            
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