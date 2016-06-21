package org.mixare;

import android.graphics.Bitmap;
import android.location.Location;

import org.mixare.Marker;
import org.mixare.data.DataSource;
import org.mixare.data.DataSource.DATASOURCE;
import org.mixare.gui.PaintScreen;

/**
 * Created by siosi on 2016-05-31.
 */
public class SnsMarker extends Marker {

    public static final int MAX_OBJECTS=50;

 //   DataSource.DATAFLAG temp;

    public SnsMarker(String title, double latitude, double longitude, double altitude, String link, DATASOURCE datasource, String body,String postTime) {
        super(title, latitude, longitude, altitude, link, datasource);
        body="";
        postTime= "";
       // temp = DataSource.DATAFLAG.SNS;

    }

    // 마커 갱신
    @Override
    public void update(Location curGPSFix) {

        //0.35 radians ~= 20 degree
        //0.85 radians ~= 45 degree
        //minAltitude = sin(0.35)
        //maxAltitude = sin(0.85)


        // TODO: 2016-05-31  플래그에 따라서 갱신내용 달리해줘야한다.
        double altitude = curGPSFix.getAltitude()+Math.sin(0.35)*distance+Math.sin(0.4)*(distance/(MixView.dataView.getRadius()*1000f/distance));
        mGeoLoc.setAltitude(altitude - 0.2);
        super.update(curGPSFix);

    }

    // 페인트 스크린에 마커 출력
    @Override
    public void draw(PaintScreen dw) {

        // 텍스트 블록을 그린다
        drawTextBlock(dw,datasource);
        // 보여지는 상황이라면
        if (isVisible) {
            float maxHeight = Math.round(dw.getHeight() / 10f) + 1;    // 최대 높이 계산
            // 데이터 소스의 비트맵 파일을 읽어온다
            Bitmap bitmap = DataSource.getBitmap("SNS");

            // 비트맵 파일이 읽혔다면 적절한 위치에 출력
            if (bitmap != null) {
                dw.paintBitmap(bitmap, cMarker.x - maxHeight / 1.5f, cMarker.y - maxHeight / 1.5f);
            } else {    // 비트맵 파일을 갖지 않는 마커의 경우
                dw.setStrokeWidth(maxHeight / 10f);
                dw.setFill(false);
                dw.setColor(DataSource.getColor(datasource));
                dw.paintCircle(cMarker.x, cMarker.y, maxHeight / 1.5f);
            }
        }
    }

    @Override
    public int getMaxObjects() {
        return MAX_OBJECTS;
    }

    // TODO: 2016-05-31 글쓰기 기능을 만들어야한다. 
    // TODO: 2016-05-31 데이터 소스를 변경했을때 바로바로 전체 마커들이 갱신이 되는가?
    // TODO: 2016-05-31  서버로 보내기 그런것도 완료해야됨. 

}
