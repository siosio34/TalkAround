/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package org.mixare;

import org.mixare.data.DataSource;
import org.mixare.data.DataSource.DATASOURCE;
import org.mixare.gui.PaintScreen;

import android.graphics.Path;
import android.location.Location;

/**
 * @author hannes
 *
 */
// 네비게이션 마커 클래스. 마커로부터 확장
public class NavigationMarker extends Marker {
	
	public static final int MAX_OBJECTS=10;	// 최대 객체 수

	// 생성자. 타이틀, 위도, 경도, 고도, 그리고 URL과 데이터 소스를 인자로 받는다
	public NavigationMarker(String title, double latitude, double longitude,
			double altitude, String URL, DATASOURCE datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);
		// TODO Auto-generated constructor stub
	}



	// 마커 위치 갱신
	@Override
	public void update(Location curGPSFix) {
	
		super.update(curGPSFix);
		
		// 마커의 위치를 갱신한다. 내용 아래쪽으로... 
		// we want the navigation markers to be on the lower part of
		// your surrounding sphere so we set the height component of 
		// the position vector radius/2 (in meter) below the user

		locationVector.y-=MixView.dataView.getRadius()*500f;
		//locationVector.y+=-1000;
	}

	// 페인트 스크린에 마커 출력. 화살표와 텍스트 블록을 그린다 
	@Override
	public void draw(PaintScreen dw) {
		drawArrow(dw);
		drawTextBlock(dw,datasource);
	}
	
	// 화살표를 그림
	public void drawArrow(PaintScreen dw) {
		// 보여지는 상황이라면
		if (isVisible) {
			// 현재각과 최대높이
			float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);
			float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

			// 페인트 스크린을 설정하고
			dw.setColor(DataSource.getColor(datasource));
			dw.setStrokeWidth(maxHeight / 10f);
			dw.setFill(false);
			
			// 패스에 따라 그린다
			Path arrow = new Path();
			float radius = maxHeight / 1.5f;
			float x=0;
			float y=0;
			arrow.moveTo(x-radius/3, y+radius);
			arrow.lineTo(x+radius/3, y+radius);
			arrow.lineTo(x+radius/3, y);
			arrow.lineTo(x+radius, y);
			arrow.lineTo(x, y-radius);
			arrow.lineTo(x-radius, y);
			arrow.lineTo(x-radius/3,y);
			arrow.close();
			dw.paintPath(arrow,cMarker.x,cMarker.y,radius*2,radius*2,currentAngle+90,1);			
		}
	}

	// 최대 객채 수 리턴
	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}
}
