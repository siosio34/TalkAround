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

import org.mixare.data.DataSource.DATASOURCE;

import android.location.Location;

/**
 * @author hannes
 *
 */
// POI(Point of Interest)마커 클래스. 마커로부터 확장
public class POIMarker extends Marker {
	
	public static final int MAX_OBJECTS=20;	// 최대 객체 수

	// 생성자. 타이틀과 위도, 경도, 고도, 그리고 URL과 데이터 소스를 인자로 받는다
	public POIMarker(String title, double latitude, double longitude,
			double altitude, String URL, DATASOURCE datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);
		// TODO Auto-generated constructor stub
	}

	// 마커 위치 갱신
	@Override
	public void update(Location curGPSFix) {
		super.update(curGPSFix);
	}

	// 최대 객체 수 리턴
	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

}
