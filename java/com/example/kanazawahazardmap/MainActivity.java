package com.example.kanazawahazardmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 金沢市ハザードマップ案内アプリ
 * 浸水タイルの表示、避難所検索、複数ルートの描画
 */
public class MainActivity extends AppCompatActivity {

    private MapView map;
    private TextView textView1;
    private TextView textView2;

    private Marker startMarker;
    private Marker nearestShelterMarker;
    private final List<Marker> shelterMarkers = new ArrayList<>();
    private final List<Polyline> roadOverlays = new ArrayList<>();
    private final List<Marker> labelMarkers = new ArrayList<>();

    // ネットワーク通信をメインスレッド以外で実行するためのサービス
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final android.os.Handler messageHandler = new android.os.Handler();
    private Runnable hideTextRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // osmdroidライブラリの初期化
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));

        map = findViewById(R.id.map);
        textView1 = findViewById(R.id.textView1);
        Button toggleButton = findViewById(R.id.toggleButton);
        textView2 = findViewById(R.id.textView2);

        // 地図の基本設定（タイルソース、初期ズーム、初期位置）
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(16.5);
        map.getController().setCenter(new GeoPoint(36.53848, 136.63304));

        // 操作ガイド（textView2）を一定時間で消去する設定
        hideTextRunnable = () -> textView2.setVisibility(View.GONE);

        toggleButton.setOnClickListener(v -> {
            if (textView2.getVisibility() == View.VISIBLE) {
                textView2.setVisibility(View.GONE);
                messageHandler.removeCallbacks(hideTextRunnable);
            } else {
                textView2.setVisibility(View.VISIBLE);
                textView2.setText("操作ガイド:\n・マップ長押しで出発地設定\n・ルートをタップで詳細表示");
                messageHandler.postDelayed(hideTextRunnable, 8000);
            }
        });

        // 浸水深タイル（国土地理院）を半透明で地図に重ねる
        MapTileProviderBasic provider = new MapTileProviderBasic(getApplicationContext(), new FloodTileSource());
        TilesOverlay floodTiles = new TilesOverlay(provider, this);
        floodTiles.setLoadingBackgroundColor(Color.TRANSPARENT);
        AlphaTilesOverlay alphaFlood = new AlphaTilesOverlay(floodTiles, 150);
        map.getOverlays().add(alphaFlood);

        // アセット内のCSVから避難所データを読み込み
        loadSheltersFromCSV();

        // 地図上のイベント検知：空白タップでラベルを閉じ、長押しで地点を設定
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                closeAllInfoWindows();
                return false;
            }
            @Override public boolean longPressHelper(GeoPoint p) {
                setStartPoint(p);
                return true;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(receiver));

        // UI拡張：右下の凡例サムネイルと中央の凡例オーバーレイのセットアップ
        setupLegendButton();
        showFloodDepthImageWithStyle();

        // 起動時に自動で操作ガイドを一度表示
        toggleButton.performClick();
    }

    /**
     * 画面右下に凡例表示を起動するための小さなボタンを作成
     */
    private void setupLegendButton() {
        FrameLayout root = findViewById(android.R.id.content);
        ImageView legendButton = new ImageView(this);
        legendButton.setImageResource(R.drawable.flood_depth_chart);
        legendButton.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // 白い角丸背景のデザイン設定
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(16);
        shape.setColor(Color.WHITE);
        shape.setStroke(2, Color.LTGRAY);
        legendButton.setBackground(shape);

        int sizeWidth = (int) (80 * getResources().getDisplayMetrics().density);
        int sizeHeight = (int) (60 * getResources().getDisplayMetrics().density);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizeWidth, sizeHeight);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, margin, margin + 100);

        legendButton.setOnClickListener(v -> showFloodDepthImageWithStyle());
        root.addView(legendButton, params);
    }

    /**
     * 浸水深の凡例を中央に表示
     */
    private void showFloodDepthImageWithStyle() {
        FrameLayout root = findViewById(android.R.id.content);
        if (root.findViewWithTag("LegendOverlay") != null) return;

        FrameLayout overlayContainer = new FrameLayout(this);
        overlayContainer.setTag("LegendOverlay");
        overlayContainer.setBackgroundColor(Color.parseColor("#AA000000"));

        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.flood_depth_chart);
        imageView.setPadding(10, 10, 10, 10);
        imageView.setBackgroundColor(Color.WHITE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(30, 30, 30, 30);

        overlayContainer.addView(imageView, params);
        root.addView(overlayContainer);

        overlayContainer.setAlpha(0f);
        overlayContainer.animate().alpha(1f).setDuration(300).start();

        overlayContainer.setOnClickListener(v -> overlayContainer.animate()
                .alpha(0f).setDuration(300)
                .withEndAction(() -> root.removeView(overlayContainer)).start());
    }

    /**
     * CSVファイルから避難所情報を読み込み、ic_shelter_pictでマーカーを設置
     */
    private void loadSheltersFromCSV() {
        Drawable original = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shelter_pict, null);
        Drawable scaledIcon = null;
        if (original != null && original instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) original).getBitmap();
            float density = getResources().getDisplayMetrics().density;
            int targetWidth = (int) (12 * density);
            int targetHeight = (int) (targetWidth * ((float) bitmap.getHeight() / bitmap.getWidth()));
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            scaledIcon = new BitmapDrawable(getResources(), scaledBitmap);
        }

        try (InputStream is = getAssets().open("shelters.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length >= 3) {
                    String name = cols[0].trim();
                    // 公園などは一時避難所であるため、建物指定のリストから除外する処理
                    if (name.contains("公園") || name.contains("緑地") || name.contains("広場") ||
                            name.contains("テニスコート") || name.contains("グラウンド") || name.contains("緑道") ||
                            name.contains("遊園") || name.contains("森")) continue;

                    Marker marker = new Marker(map);
                    marker.setPosition(new GeoPoint(Double.parseDouble(cols[1]), Double.parseDouble(cols[2])));
                    marker.setTitle(name);
                    if (scaledIcon != null) marker.setIcon(scaledIcon);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(marker);
                    shelterMarkers.add(marker);
                }
            }
        } catch (IOException e) {
            Log.e("MainActivity", "CSV読み込み失敗", e);
        }
    }

    /**
     * 長押し地点を出発地(marker_orange)として設定し、経路検索を実行
     */
    private void setStartPoint(GeoPoint point) {
        if (startMarker != null) map.getOverlays().remove(startMarker);
        startMarker = new Marker(map);
        startMarker.setPosition(point);

        Drawable startDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_orange, null);
        if (startDrawable != null && startDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) startDrawable).getBitmap();
            float density = getResources().getDisplayMetrics().density;
            int targetWidth = (int) (18 * density);
            int targetHeight = (int) (targetWidth * ((float) bitmap.getHeight() / bitmap.getWidth()));
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            startMarker.setIcon(new BitmapDrawable(getResources(), scaledBitmap));
        }
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("出発地");
        map.getOverlays().add(startMarker);

        nearestShelterMarker = findNearestShelter(point);
        if (nearestShelterMarker != null) requestRoutes(point, nearestShelterMarker.getPosition());
        map.invalidate();
    }

    /**
     * 最も近い避難所を直線距離で判定
     */
    private Marker findNearestShelter(GeoPoint start) {
        Marker nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Marker shelter : shelterMarkers) {
            double dist = start.distanceToAsDouble(shelter.getPosition());
            if (dist < minDist) {
                minDist = dist;
                nearest = shelter;
            }
        }
        return nearest;
    }

    /**
     * 地図上に表示されている全ての候補ラベルを閉じます
     */
    private void closeAllInfoWindows() {
        for (Marker marker : labelMarkers) {
            if (marker.isInfoWindowShown()) {
                marker.closeInfoWindow();
            }
        }
    }

    /**
     * OSRM APIを使用して経路を算出。メインは赤、代替は青で描画します。
     * ルートをタップすると、そのルートの中間地点に候補ラベルを表示する仕組みです。
     */
    private void requestRoutes(GeoPoint start, GeoPoint destination) {
        executorService.execute(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, "MyUserAgent");
            roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT);
            roadManager.addRequestOption("alternatives=true");

            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(start);
            waypoints.add(destination);

            final Road[] roads = roadManager.getRoads(waypoints);

            runOnUiThread(() -> {
                closeAllInfoWindows();
                for (Polyline oldLine : roadOverlays) map.getOverlays().remove(oldLine);
                for (Marker oldLabel : labelMarkers) map.getOverlays().remove(oldLabel);
                roadOverlays.clear();
                labelMarkers.clear();

                if (roads == null) return;

                // 描画順を制御し、メインルート(i=0)が最前面
                for (int i = roads.length - 1; i >= 0; i--) {
                    if (roads[i].mStatus == Road.STATUS_OK) {
                        Polyline line = RoadManager.buildRoadOverlay(roads[i]);

                        // ラベル用の見えないマーカーをルートの中間地点に配置
                        Marker labelMarker = new Marker(map);
                        int midPointIndex = roads[i].mRouteHigh.size() / 2;
                        labelMarker.setPosition(roads[i].mRouteHigh.get(midPointIndex));
                        labelMarker.setIcon(new ColorDrawable(Color.TRANSPARENT));
                        labelMarker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

                        // Polylineオブジェクトとラベルマーカーをリンク
                        line.setRelatedObject(labelMarker);

                        // ルートの線をタップした際のイベント処理
                        line.setOnClickListener((polyline, mapView, eventPos) -> {
                            Marker associatedMarker = (Marker) polyline.getRelatedObject();
                            if (associatedMarker != null) {
                                boolean wasShown = associatedMarker.isInfoWindowShown();
                                closeAllInfoWindows(); // 一旦全て閉じてから表示切替
                                if (!wasShown) {
                                    associatedMarker.showInfoWindow();
                                }
                            }
                            return true;
                        });

                        if (i == 0) {
                            // 最短ルートの設定（赤色）
                            line.getOutlinePaint().setColor(Color.RED);
                            line.getOutlinePaint().setStrokeWidth(12f);

                            // 徒歩予測時間の計算（分速80m基準）
                            double distanceInMeters = roads[i].mLength * 1000;
                            int minutes = (int) Math.ceil(distanceInMeters / 80.0);

                            textView1.setText(String.format("最寄り: %s\n距離: %.0fm / 予想時間: 約%d分",
                                    nearestShelterMarker.getTitle(), distanceInMeters, minutes));

                            labelMarker.setTitle("候補1 (最短)");
                        } else {
                            // 代替ルートの設定（青色・半透明）
                            line.getOutlinePaint().setColor(Color.BLUE);
                            line.getOutlinePaint().setStrokeWidth(8f);
                            line.getOutlinePaint().setAlpha(150);

                            labelMarker.setTitle("候補" + (i + 1));
                        }

                        map.getOverlays().add(line);
                        roadOverlays.add(line);

                        map.getOverlays().add(labelMarker);
                        labelMarkers.add(labelMarker);
                    }
                }
                map.invalidate();
                Toast.makeText(MainActivity.this, "ルートをタップすると詳細が表示されます", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override protected void onPause() { super.onPause(); if (map != null) map.onPause(); }

    /**
     * 国土地理院の浸水深データをタイル形式で取得
     */
    public static class FloodTileSource extends OnlineTileSourceBase {
        public FloodTileSource() {
            super("FloodTiles", 0, 18, 256, ".png",
                    new String[]{"https://disaportaldata.gsi.go.jp/raster/01_flood_l2_shinsuishin_data/"});
        }
        @Override
        public String getTileURLString(long pMapTileIndex) {
            return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                    MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) + ".png";
        }
    }

    /**
     * タイル全体のアルファ（透明度）を制御するためのオーバレイ
     */
    public static class AlphaTilesOverlay extends Overlay {
        private final TilesOverlay inner;
        private final int alpha;
        public AlphaTilesOverlay(TilesOverlay inner, int alpha) { this.inner = inner; this.alpha = alpha; }
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            RectF bounds = new RectF(0, 0, mapView.getWidth(), mapView.getHeight());
            int save = canvas.saveLayerAlpha(bounds, alpha);
            try { inner.draw(canvas, mapView, shadow); }
            finally { canvas.restoreToCount(save); }
        }
    }
}