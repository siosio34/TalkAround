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
package org.mixare.reality;

import org.mixare.render.MixVector;

import android.location.Location;

import java.io.Serializable;

// 물리적 공간 클래스
public class PhysicalPlace implements Serializable{
	
	// 위치 정보
	double latitude;	// 위도
	double longitude;	// 경도
	double altitude;	// 고도

	// 기본 생성자
	public PhysicalPlace() {

	}

	// 생성자. 객체로 받아와 복사
	public PhysicalPlace(PhysicalPlace pl) {
		this.setTo(pl.latitude, pl.longitude, pl.altitude);
	}

	// 생성자. 각 값을 받아 설정
	public PhysicalPlace(double latitude, double longitude, double altitude) {
		this.setTo(latitude, longitude, altitude);
	}

	// 위도, 경도, 고도 각 값을 설정
	public void setTo(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	// 객체로 받아와서 값을 대입
	public void setTo(PhysicalPlace pl) {
		this.latitude = pl.latitude;
		this.longitude = pl.longitude;
		this.altitude = pl.altitude;
	}

	// 문자열 형태로 위도, 경도, 고도값의 정보를 출력
	@Override
	public String toString() {
		return "(lat=" + latitude + ", lng=" + longitude + ", alt=" + altitude
				+ ")";
	}

	// 위도에 대한 게터와 세터
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	// 경도에 대한 게터와 세터
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	// 고도에 대한 게터와 세터
	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	
	// 목적지의 위치를 계산. 위도와 경도, bear 값과 거리(d) 등이 쓰인다
	public static void calcDestination(double lat1Deg, double lon1Deg,
			double bear, double d, PhysicalPlace dest) {
		/** see http://en.wikipedia.org/wiki/Great-circle_distance */
		
		// 각 값들을 라디안으로 변환하여 임시변수에 저장
		double brng = Math.toRadians(bear);
		double lat1 = Math.toRadians(lat1Deg);
		double lon1 = Math.toRadians(lon1Deg);
		double R = 6371.0 * 1000.0; 

		// 저장된 값들로 목적지의 위치를 계산
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
						Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));

		// 계산된 값은 목적지 물리적 장소 객체 dest에 설정된다 
		dest.setLatitude(Math.toDegrees(lat2));
		dest.setLongitude(Math.toDegrees(lon2));
	}

	// 위치값을 벡터값으로 바꾸어 줌. Android의 Location객체를 이용(?)
	public static void convLocToVec(Location org, PhysicalPlace gp,
			MixVector v) {
		
		float[] z = new float[1];
		z[0] = 0;
		Location.distanceBetween(org.getLatitude(), org.getLongitude(), gp
				.getLatitude(), org.getLongitude(), z);
		
		float[] x = new float[1];
		Location.distanceBetween(org.getLatitude(), org.getLongitude(), org
				.getLatitude(), gp.getLongitude(), x);
		
		double y = gp.getAltitude() - org.getAltitude();
		
		if (org.getLatitude() < gp.getLatitude())
			z[0] *= -1;
		if (org.getLongitude() > gp.getLongitude())
			x[0] *= -1;

		// 계산된 값을 벡터에 설정
		v.set(x[0], (float) y, z[0]);
	}

	// 벡터값을 위치값으로 바꾸어 줌. Android 의 Location 객체를 이용(?)
	public static void convertVecToLoc(MixVector v, Location org, Location gp) {
		double brngNS = 0, brngEW = 90;
		if (v.z > 0)
			brngNS = 180;
		if (v.x < 0)
			brngEW = 270;

		PhysicalPlace tmp1Loc = new PhysicalPlace();
		PhysicalPlace tmp2Loc = new PhysicalPlace();
		
		PhysicalPlace.calcDestination(org.getLatitude(), org.getLongitude(), brngNS,
				Math.abs(v.z), tmp1Loc);
		PhysicalPlace.calcDestination(tmp1Loc.getLatitude(), tmp1Loc.getLongitude(),
				brngEW, Math.abs(v.x), tmp2Loc);

		// 계산된 값을 Location 객체에 설정
		gp.setLatitude(tmp2Loc.getLatitude());
		gp.setLongitude(tmp2Loc.getLongitude());
		gp.setAltitude(org.getAltitude() + v.y);
	}
}
