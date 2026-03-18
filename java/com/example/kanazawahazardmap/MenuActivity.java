package com.example.kanazawahazardmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ボタン（LinearLayout）を取得
        LinearLayout btnMap = findViewById(R.id.btnMap);
        LinearLayout btnWeather = findViewById(R.id.btnWeather);
        LinearLayout btnQuiz = findViewById(R.id.btnQuiz);

        // 地図モードへの遷移
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // 気象情報への遷移
        btnWeather.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, WeatherActivity.class);
            startActivity(intent);
        });

        // 防災クイズへの遷移
        btnQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, QuizActivity.class);
            startActivity(intent);
        });
    }
}