package com.example.kanazawahazardmap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private int currentQuestionIndex = 0;
    private int score = 0;
    private final List<Question> quizList = new ArrayList<>();
    private final int MAX_QUESTIONS = 5;

    private TextView textProgress, textQuestion, textResultIcon, textResultMessage;
    private Button btn1, btn2, btn3, btnNext;
    private CardView cardResult;
    private LinearLayout layoutResultBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        loadQuizDataFromJSON();

        textProgress = findViewById(R.id.textProgress);
        textQuestion = findViewById(R.id.textQuestion);
        cardResult = findViewById(R.id.cardResult);
        textResultIcon = findViewById(R.id.textResultIcon);
        textResultMessage = findViewById(R.id.textResultMessage);
        layoutResultBackground = findViewById(R.id.layoutResultBackground);
        btnNext = findViewById(R.id.btnNext);
        btn1 = findViewById(R.id.btnAnswer1);
        btn2 = findViewById(R.id.btnAnswer2);
        btn3 = findViewById(R.id.btnAnswer3);

        if (!quizList.isEmpty()) {
            loadQuestion();
        }

        btn1.setOnClickListener(v -> checkAnswer(0));
        btn2.setOnClickListener(v -> checkAnswer(1));
        btn3.setOnClickListener(v -> checkAnswer(2));

        btnNext.setOnClickListener(v -> {
            cardResult.setVisibility(View.GONE);
            currentQuestionIndex++;

            if (currentQuestionIndex < quizList.size() && currentQuestionIndex < MAX_QUESTIONS) {
                loadQuestion();
            } else {
                Intent intent = new Intent(QuizActivity.this, QuizResultActivity.class);
                intent.putExtra("SCORE", score);
                intent.putExtra("TOTAL", Math.min(quizList.size(), MAX_QUESTIONS));
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadQuizDataFromJSON() {
        try {
            InputStream is = getAssets().open("quiz_data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONArray choicesArray = obj.getJSONArray("choices");
                String[] choices = new String[3];
                for (int j = 0; j < 3; j++) {
                    choices[j] = choicesArray.getString(j);
                }
                quizList.add(new Question(
                        obj.getString("question"),
                        choices,
                        obj.getInt("correctIndex")
                ));
            }

            Collections.shuffle(quizList);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadQuestion() {
        Question q = quizList.get(currentQuestionIndex);
        int displayTotal = Math.min(quizList.size(), MAX_QUESTIONS);
        textProgress.setText(String.format("第 %d 問 / 全 %d 問", currentQuestionIndex + 1, displayTotal));
        textQuestion.setText(q.question);
        btn1.setText(q.choices[0]);
        btn2.setText(q.choices[1]);
        btn3.setText(q.choices[2]);
    }

    private void checkAnswer(int selectedIndex) {
        boolean isCorrect = (selectedIndex == quizList.get(currentQuestionIndex).correctIndex);
        cardResult.setVisibility(View.VISIBLE);

        if (isCorrect) {
            score++;
            textResultIcon.setText("○");
            textResultMessage.setText("正解です！");
            layoutResultBackground.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            textResultIcon.setText("×");
            textResultMessage.setText("残念、不正解です");
            layoutResultBackground.setBackgroundColor(Color.parseColor("#F44336"));
        }
    }

    static class Question {
        String question;
        String[] choices;
        int correctIndex;

        Question(String q, String[] c, int correct) {
            question = q;
            choices = c;
            correctIndex = correct;
        }
    }
}