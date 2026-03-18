package com.example.kanazawahazardmap;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * 洪水浸水タイルを取得するための TileSource クラス
 */
public class FloodTileSource extends OnlineTileSourceBase {

    public FloodTileSource() {
        super("FloodTiles", 0, 18, 256, ".png",
                new String[]{"https://disaportal.gsi.go.jp/data/raster/01_flood_l2_shinsuishin_data/"});
    }

    @Override
    public String getTileURLString(long pMapTileIndex) {
        int zoom = MapTileIndex.getZoom(pMapTileIndex);
        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);
        return getBaseUrl() + zoom + "/" + x + "/" + y + ".png";
    }
}
