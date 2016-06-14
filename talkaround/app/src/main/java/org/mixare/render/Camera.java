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
package org.mixare.render;

// 카메라 부분을 담당하는 클래스
public class Camera {
	
	// 기본 뷰의 각도
	public static final float DEFAULT_VIEW_ANGLE = (float) Math.toRadians(45);
	
	// 넓이와 높이
	public int width, height;

	// 변환행렬과 벡터
	public Matrix transform = new Matrix();
	public MixVector lco = new MixVector();

	// 뷰의 각도와 거리
	float viewAngle;
	float dist;

	// 기본 생성자. 넓이와 높이를 설정
	public Camera(int width, int height) {
		this(width, height, true);
	}

	// 생성자. 넓이와 높이, 세팅 상태를 설정
	public Camera(int width, int height, boolean init) {
		this.width = width;
		this.height = height;

		// 변환행렬과 벡터를 단위행렬, 0벡터로 설정
		transform.toIdentity();
		lco.set(0, 0, 0);
	}

	// 뷰의 각도를 설정. 인자는 각도
	public void setViewAngle(float viewAngle) {
		this.viewAngle = viewAngle;

		// 거리는 넓이의 절반을 tan(각도)의 절반으로 나눈 것
		this.dist = (this.width / 2)
				/ (float) Math.tan(viewAngle / 2);
	}

	// 뷰의 각도를 설정. 넓이와 높이도 함께 인자로 받는다
	public void setViewAngle(int width, int height, float viewAngle) {
		// 값 설정은 위와 동일
		this.viewAngle = viewAngle;
		this.dist = (width / 2) / (float) Math.tan(viewAngle / 2);
	}

	// 사영점을 설정(벡터 사영)
	// 인자값으로는 본점(origin)과 투영점(dest), 투영점 계산에 사용될 x,y 값을 받음 
	public void projectPoint(MixVector orgPoint, MixVector prjPoint, float addX,
			float addY) {
		prjPoint.x = dist * orgPoint.x / -orgPoint.z;
		prjPoint.y = dist * orgPoint.y / -orgPoint.z;
		prjPoint.z = orgPoint.z;
		prjPoint.x = prjPoint.x + addX + width / 2;
		prjPoint.y = -prjPoint.y + addY + height / 2;
	}
	
	// 문자열 형태로 카메라의 정보를 출력
	@Override
	public String toString() {
		return "CAM(" + width + "," + height + ")";
	}
}
