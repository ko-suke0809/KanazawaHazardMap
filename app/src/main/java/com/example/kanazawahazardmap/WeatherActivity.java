package com.example.kanazawahazardmap;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 気象情報を表示する画面
 * WebViewを使用して外部の気象サイトを表示
 */
public class WeatherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // xmlのWebViewを取得
        WebView webView = findViewById(R.id.webViewWeather);

        // アプリ内でページが開くように設定
        webView.setWebViewClient(new WebViewClient());

        // JavaScriptを有効
        webView.getSettings().setJavaScriptEnabled(true);

        // 日本気象協会の金沢市のページを読み込み
        webView.loadUrl("https://tenki.jp/forecast/4/20/5610/17201/");
    }
}