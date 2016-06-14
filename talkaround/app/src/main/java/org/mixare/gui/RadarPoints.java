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
package org.mixare.gui;

import org.mixare.DataView;
import org.mixare.Marker;
import org.mixare.data.DataHandler;
import org.mixare.data.DataSource;

import android.graphics.Color;

/** Takes care of the small radar in the top left corner and of its points
 * @author daniele
 *
 */
// 좌상단의 레이더를 나타낼 클래스
public class RadarPoints implements ScreenObj {

	public DataView view;	// 스크린 뷰
	float range;	// 레이더의 각도
	public static float RADIUS = 40;	// 스크린 픽셀의 각도. 기본 40도
	static float originX = 0 , originY = 0;	// 스크린상의 좌표
	static int radarColor = Color.argb(100, 0, 0, 200);	// 색상. 기본은 약간 푸른색
	
	// 실제 출력을 담당하는 메소드 
	public void paint(PaintScreen dw) {
		/** radius is in KM. */
		range = view.getRadius() * 1000;
		
		// 레이더를 그린다
		dw.setFill(true);
		dw.setColor(radarColor);
		dw.paintCircle(originX + RADIUS, originY + RADIUS, RADIUS);

		// 확대 배율을 설정
		float scale = range / RADIUS;

		// 데이터를 받아온 후
		DataHandler jLayer = view.getDataHandler();

		// 마커들을 삽입
		for (int i = 0; i < jLayer.getMarkerCount(); i++) {
			// 마커를 하나씩 받아온 후,
			Marker pm = jLayer.getMarker(i);
			// x, y 값을 설정한다. 확대배율에 반비례한다.
			float x = pm.getLocationVector().x / scale;
			float y = pm.getLocationVector().z / scale;

			// 최종적으로 색 설정과 출력
			if (pm.isActive() && (x * x + y * y < RADIUS * RADIUS)) {
				dw.setFill(true);
				
				dw.setColor(DataSource.getColor(pm.getDatasource()));
				dw.paintRect(x + RADIUS - 1, y + RADIUS - 1, 2, 2);
			}
		}
	}

	// 스크린의 넓이를 반환
	public float getWidth() {
		return RADIUS * 2;
	}

	// 스크린의 높이를 반환
	public float getHeight() {
		return RADIUS * 2;
	}
}

