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

// 스크린 라인의 클래스
public class ScreenLine {
	public float x, y;	// x, y 위치값

	// 기본 생성자. x, y 값은 각각 0으로 세팅 
	public ScreenLine() {
		set(0, 0);
	}

	// 생성자. 지정한 값으로 x, y 값 설정
	public ScreenLine(float x, float y) {
		set(x, y);
	}

	// 세터. 지정한 값으로 x, y 값 설정
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	// 인자로 받은 각도로 회전시킨다
	public void rotate(double t) {
		// 삼각함수를 이용한 계산으로 정확한 값을 지정
		float xp = (float) Math.cos(t) * x - (float) Math.sin(t) * y;
		float yp = (float) Math.sin(t) * x + (float) Math.cos(t) * y;

		x = xp;
		y = yp;
	}

	// 각 x, y 값에 특정 수치를 더함
	public void add(float x, float y) {
		this.x += x;
		this.y += y;
	}
}
