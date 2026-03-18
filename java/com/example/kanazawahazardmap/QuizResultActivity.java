package com.example.kanazawahazardmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        TextView textScore = findViewById(R.id.textScore);
        TextView textComment = findViewById(R.id.textComment);
        Button btnBack = findViewById(R.id.btnBackToMenu);

        // Intentからスコアと総問題数を受け取ります
        int score = getIntent().getIntExtra("SCORE", 0);
        int total = getIntent().getIntExtra("TOTAL", 0);

        textScore.setText(score + " / " + total);

        if (score == total) {
            textComment.setText("完璧です！防災マスターですね。");
        } else if (score >= total / 2) {
            textComment.setText("素晴らしいです。その調子で学びましょう。");
        } else {
            textComment.setText("もう一度復習してみましょう。");
        }

        btnBack.setOnClickListener(v -> {
            // メニュー画面に戻ります
            Intent intent = new Intent(QuizResultActivity.this, MenuActivity.class);
            // 戻る際にこれまでの画面履歴をクリアして、二重に開かないようにします
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}