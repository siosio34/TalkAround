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

import android.location.Location;

import org.mixare.data.DataSource;
import org.mixare.gui.PaintScreen;
import org.mixare.gui.ScreenLine;
import org.mixare.gui.ScreenObj;
import org.mixare.gui.TextObj;
import org.mixare.reality.PhysicalPlace;
import org.mixare.render.Camera;
import org.mixare.render.MixVector;

import java.net.URLDecoder;
import java.text.DecimalFormat;

// 화면에 찍힐 마커를 담당할 클래스. Comparable 구현
abstract public class Marker implements Comparable<Marker> {

	private String ID;    // ID값
	protected String title;    // 타이틀
	private boolean underline = false;    // 밑줄 여부
	private String URL;    // 연동될 URL
	protected PhysicalPlace mGeoLoc;    // 물리적 공간 객체. 실제 장소값을 저장
	// 유저와 물리적 공간 간의 거리(미터 단위)
	protected double distance;
	// 이 마커가 생성된 데이터소스
	protected DataSource.DATASOURCE datasource;
	private boolean active;    // 활성화 여부

	// 드로우 속성
	protected boolean isVisible;    // 보여지는지 여부
	//	private boolean isLookingAt;
//	private boolean isNear;
//	private float deltaCenter;
	public MixVector cMarker = new MixVector();    // 카메라 마커
	protected MixVector signMarker = new MixVector();    // 기호 마커
//	private MixVector oMarker = new MixVector();

	// 장소에 관련된 벡터값들
	protected MixVector locationVector = new MixVector();
	private MixVector origin = new MixVector(0, 0, 0);
	private MixVector upV = new MixVector(0, 1, 0);
	private ScreenLine pPt = new ScreenLine();    // 클릭 지점을 판단하기 위함

	// 라벨과 화면에 표시될 텍스트 블록
	protected Label txtLab = new Label();    // Label 클래스는 하단에서 정의한다
	protected TextObj textBlock;

	//TODO : NAVER MARKER용 설명
	private String description = "";

	// 생성자. 타이틀과 위도, 경고, 고도값, 링크될 주소와 데이터 소스를 인자로 받는다 
	public Marker(String title, double latitude, double longitude, double altitude, String link, DataSource.DATASOURCE datasource) {
		super();

		this.active = true;    // 일단 비활성화 상태로

		// 각 속성값 할당
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude, longitude, altitude);
		if (link != null && link.length() > 0) {
			// 링크가 null 이 아닐 경우, 웹페이지의 형태의 링크를 추가하고 
			URL = "webpage:" + URLDecoder.decode(link);
			this.underline = false;    // 밑줄을 친다
		}
		this.datasource = datasource;

		// 마커의 ID는 '데이터소스##타이틀' 형태이다
		this.ID = datasource + "##" + title; //mGeoLoc.toString();
	}

	// 타이틀을 리턴
	public String getTitle() {
		return title;
	}

	//
	// URL을 리턴/
	public String getURL() {
		return URL;
	}

	// 위도를 리턴
	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}

	// 경도를 리턴
	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}

	// 고도를 리턴
	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}

	// 위치 벡터를 리턴
	public MixVector getLocationVector() {
		return locationVector;
	}

	// 데이터 소스를 리턴	
	public DataSource.DATASOURCE getDatasource() {
		return datasource;
	}

	// 데이터 소스를 세팅
	public void setDatasource(DataSource.DATASOURCE datasource) {
		this.datasource = datasource;
	}

	//TODO : NAVER 마커 설명 GET/SET
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	// 카메라 마커. 최초 위치와 투영될 카메라, 추가되는 x, y 값을 인자로 받는다 
	private void cCMarker(MixVector originalPoint, Camera viewCam, float addX, float addY, DataSource.DATASOURCE type) {
		// 임시 속성들
		MixVector tmpa = new MixVector(originalPoint);
		MixVector tmpc = new MixVector(upV);

		// 위치 벡터를 더하고 뷰 카메라의 벡터값은 뺀 후 
		tmpa.add(locationVector); //3 
		tmpc.add(locationVector); //3
		tmpa.sub(viewCam.lco); //4
		tmpc.sub(viewCam.lco); //4
		// 카메라의 변환 행렬을 곱한다
		tmpa.prod(viewCam.transform); //5
		tmpc.prod(viewCam.transform); //5

		// 새 임시벡터를 선언하고
		MixVector tmpb = new MixVector();
		// 계산된 벡터들로 카메라 마커와 기호 마커를 사영한다

		viewCam.projectPoint(tmpa, tmpb, addX, addY); //6
		cMarker.set(tmpb); //7

		viewCam.projectPoint(tmpc, tmpb, addX, addY); //6
		signMarker.set(tmpb); //7
	}

	// 고도를 계산. 이제는 쓰이지 않는듯?
	private void calcV(Camera viewCam) {
		isVisible = false;    // 일단 보이지 않는 상태로 만들고
//		isLookingAt = false;
//		deltaCenter = Float.MAX_VALUE;

		// 마커의 z 값에 따른 처리를 한다
		if (cMarker.z < -1f) {
			isVisible = true;

			// 카메라 마커가 현재 카메라 뷰의 공간 안에 있는지 판단 
			if (MixUtils.pointInside(cMarker.x, cMarker.y, 0, 0,
					viewCam.width, viewCam.height)) {

//				float xDist = cMarker.x - viewCam.width / 2;
//				float yDist = cMarker.y - viewCam.height / 2;
//				float dist = xDist * xDist + yDist * yDist;

//				deltaCenter = (float) Math.sqrt(dist);
//
//				if (dist < 50 * 50) {
//					isLookingAt = true;
//				}
			}
		}
	}

	// 마커 위치를 업데이트
	public void update(Location curGPSFix) {
		// 고도 0.0은 아마 POI의 고도가 알려지지 않았고,
		// 유저의 GPS 높이를 세팅해야 한다는 것을 의미한다
		// http://www.geonames.org/export/web-services.html#astergdem 를 참고하여
		// SRTM, AGDEM 또는 GTOPO30등의 DEM모델을 사용해 
		// 정확한 높이를 측정 함으로써 이 문제를 개선할 수 있을 것이다

		// 고도 값이 0.0일 경우 현재의 GPS픽스를 이용해 다시 고도값을 얻어온다
		if (mGeoLoc.getAltitude() == 0.0)
			mGeoLoc.setAltitude(curGPSFix.getAltitude());

		// compute the relative position vector from user position to POI location
		// 유저 위치로부터 POI 위치 까지의 관계 지점의 벡터를 계산한다
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	// 그려질 위치를 계산
	public void calcPaint(Camera viewCam, float addX, float addY, DataSource.DATASOURCE type) {
		cCMarker(origin, viewCam, addX, addY, type);    // 카메라 마커를 생성
		calcV(viewCam);    // 카메라의 고도를 계산
	}

//	private void calcPaint(Camera viewCam) {
//		cCMarker(origin, viewCam, 0, 0);
//	}

	// 클릭이 허용되어 있는지 조사
	private boolean isClickValid(float x, float y) {
		// 현재각. 카메라 마커의 좌표와 기호 마커의 좌표 사이의 각을 구한다 
		float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);

		// 마커가 활성되어 있지 않은 경우(AR 뷰에 표시되지 않은 경우)
		if (!isActive())
			return false;    // 클릭을 체크할 필요가 없으므로 false

		//TODO adapt the following to the variable radius!

		// 기호 마커와 라벨의 위치로 클릭 부분을 계산
		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += txtLab.getX();
		pPt.y += txtLab.getY();

		// 라벨의 위치로 클릭 가능 영역을 계산한다
		float objX = txtLab.getX() - txtLab.getWidth() / 2;
		float objY = txtLab.getY() - txtLab.getHeight() / 2;
		float objW = txtLab.getWidth();
		float objH = txtLab.getHeight();

		// 가능한 영역을 클릭했는지 판단 후 리턴
		if (pPt.x > objX && pPt.x < objX + objW && pPt.y > objY
				&& pPt.y < objY + objH) {
			return true;
		} else {
			return false;
		}
	}

	// 스크린에 실제로 그려주는 메소드
	public void draw(PaintScreen dw) {
		drawCircle(dw);
		drawTextBlock(dw,datasource);
	}

	// 스크린에 원을 그린다
	public void drawCircle(PaintScreen dw) {
		// 마커가 표시중인 상태일 경우 출력
		if (isVisible) {
			// 우선 페인트 스크린을 설정한다
			//float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
			float maxHeight = dw.getHeight();
			dw.setStrokeWidth(maxHeight / 100f);
			dw.setFill(false);
			dw.setColor(DataSource.getColor(datasource));

			//draw circle with radius depending on distance
			//0.44 is approx. vertical fov in radians 
			double angle = 2.0 * Math.atan2(10, distance);
			double radius = Math.max(Math.min(angle / 0.44 * maxHeight, maxHeight), maxHeight / 25f);
			//double radius = angle/0.44d * (double)maxHeight;


			dw.paintCircle(cMarker.x, cMarker.y, (float) radius);
		}
	}

	// 텍스트 블록을 그린다. 일반적으로 URL 등을 담고있는 데이터 소스 등에 사용된다
	public void drawTextBlock(PaintScreen dw,DataSource.DATASOURCE datasource) {
		//TODO: 그려지게 될 상한선(최대높이)를 지정
		float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		//TODO: 거리가 변경되었을 경우에만 텍스트 블록을 변경한다
		String textStr = "";    // 출력될 텍스트
		double d = distance;    // 거리. 미터 단위
		DecimalFormat df = new DecimalFormat("@#");    // 숫자 포맷은 @숫자

		if(datasource != DataSource.DATASOURCE.SNS) {
			// 위치에 따른 자신과의 거리 출력. 1000m 이상은 km로 대체한다
			if (d < 1000.0) {
				textStr = title + " \n" + df.format(d) + "m";
			} else {
				d = d / 1000.0;
				textStr = title + " \n" + df.format(d) + "km";
			}
		}

		else {
			if (d < 1000.0) {
				textStr =  df.format(d) + "m";
			} else {
				d = d / 1000.0;
				textStr =  df.format(d) + "km";
			}
		}

		// 텍스트 블록(텍스트 오브젝트) 생성
		textBlock = new TextObj(textStr, Math.round(maxHeight / 3f) + 1,
				250, dw, underline);

		// 출력되는 상황일 경우
		if (isVisible) {
			// 데이터 소스에 따른 컬러를 지정
			dw.setColor(DataSource.getColor(datasource));

			// 현재 각을 얻어온다
			float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y, signMarker.x, signMarker.y);

			// 세팅된 텍스트 블록으로 텍스트 라벨을 준비
			txtLab.prepare(textBlock);

			// 페인트 스크린을 설정하고 
			dw.setStrokeWidth(1f);
			dw.setFill(true);

			// 준비된 값으로 객체를 스크린에 그린다
			dw.paintObj(txtLab, signMarker.x - txtLab.getWidth()
					/ 2, signMarker.y + maxHeight, currentAngle + 90, 1);
		}

	}

	// 데이터 뷰에서 터치시의 이벤트 처리 여부를 리턴
	// // TODO: 2016-04-13 나중에 클릭시 그 사이트 홈페이지가 뜨게 구현 합니당 0_<
	public boolean fClick(float x, float y, MixContext ctx, MixState state) {
		boolean evtHandled = false;

		if (isClickValid(x, y)) {    // 클릭 가능한 지점인 경우(클릭된 걸로 파악된 경우)
			// TODO: 2016-05-31 마커의 정보를 여기서 다띄워야 할거 같다.
			// TODO: 여기서 리퀘스트 하는 함수도 만들어야될것같다.

			evtHandled = state.handleEvent(ctx, URL, title, mGeoLoc);	// 마커의 URL 을 넘겨 이벤트 처리
		}
		return evtHandled;    // 성공했을 경우 true 를 리턴할 것이다
	}

	// 거리를 리턴
	public double getDistance() {
		return distance;
	}

	// 거리를 세팅
	public void setDistance(double distance) {
		this.distance = distance;
	}

	// ID를 리턴
	public String getID() {
		return ID;
	}

	// ID를 세팅
	public void setID(String iD) {
		ID = iD;
	}

	// 두 마커를 비교한다. 정확하게는 두 마커의 거리를 비교하여 동일한지 판단한다
	@Override
	public int compareTo(Marker another) {

		Marker leftPm = this;
		Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());

	}

	// 두 마커가 동일한지 ID로 판단한다
	@Override
	public boolean equals(Object marker) {
		return this.ID.equals(((Marker) marker).getID());
	}

	// 활성화 상태를 리턴
	public boolean isActive() {
		return active;
	}

	// 활성화 상태를 세팅
	public void setActive(boolean active) {
		this.active = active;
	}

	// 아직 미사용
	abstract public int getMaxObjects();


	// 스크린에 출력될 라벨 클래스
	class Label implements ScreenObj {

		private float x, y;    // 위치
		private float width, height;    // 넓이와 높이
		private ScreenObj obj;    // 표시될 객체

		// 표시될 객체를 준비한다
		public void prepare(ScreenObj drawObj) {
			obj = drawObj;
			float w = obj.getWidth();
			float h = obj.getHeight();

			x = w / 2;
			y = 0;

			width = w * 2;
			height = h * 2;
		}

		// 객체(라벨) 출력
		public void paint(PaintScreen dw) {
			dw.paintObj(obj, x, y, 0, 1);
		}

		// x 위치를 리턴
		public float getX() {
			return x;
		}

		// y 위치를 리턴
		public float getY() {
			return y;
		}

		// 넓이를 리턴
		public float getWidth() {
			return width;
		}

		// 높이를 리턴
		public float getHeight() {
			return height;
		}
	}
}