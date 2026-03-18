package com.example.kanazawahazardmap;

public class Shelter {
    private String name;       // 避難所名
    private double latitude;   // 緯度
    private double longitude;  // 経度

    // コンストラクタ
    public Shelter(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // getterメソッド
    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}