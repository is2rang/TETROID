package com.is2rang.tetroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class LaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);

        EditText roomInput = new EditText(this);
        roomInput.setHint("Room Code");
        roomInput.setText("");

        Button playBtn = new Button(this);
        playBtn.setText("PLAY");

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
}