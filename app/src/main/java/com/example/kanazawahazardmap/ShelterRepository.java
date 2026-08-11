package com.example.kanazawahazardmap;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ShelterRepository {

    public static List<Shelter> loadShelters(Context context) {
        List<Shelter> shelters = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("shelters.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 先頭行（ヘッダー）はスキップ
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String name = tokens[0].trim();
                    double lat = Double.parseDouble(tokens[1].trim());
                    double lon = Double.parseDouble(tokens[2].trim());
                    shelters.add(new Shelter(name, lat, lon));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shelters;
    }
}