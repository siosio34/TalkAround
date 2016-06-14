package org.mixare;

import org.mixare.data.DataSource;

/**
 * Created by PNY on 2016-06-06.
 */
public class NaverMarker extends Marker {

    public static final int MAX_OBJECTS=30;	// 최대 객체 수

    public NaverMarker(String title, double latitude, double longitude, double altitude, String link, DataSource.DATASOURCE datasource, double distance, String description) {
        super(title, latitude, longitude, altitude, link, datasource);
        super.setDistance(distance);
        super.setDescription(description);
    }

    @Override
    public int getMaxObjects() {
        return MAX_OBJECTS;
    }
}
