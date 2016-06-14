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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

// 스크린 출력을 담당하는 클래스
public class PaintScreen {

	Canvas canvas;	// 출력에 사용될 캔버스
	int width, height;	// 넓이와 높이
	Paint paint = new Paint();	// 출력에 사용될 페인트 객체
	Paint bPaint = new Paint();	// 두번째

	// 생성자. 기본 텍스트크기 16, 안티 알리아싱, 색상은 블루, 페인트 스타일은 외각선.
	public PaintScreen() {
		paint.setTextSize(16);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
	}

	// 캔버스에 대한 게터와 세터
	public Canvas getCanvas() {
		return canvas;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	// 넓이와 높이에 대한 게터와 세터
	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	// 페인트 객체의 채우기 여부를 결정. 인자로 boolean 값을 넘긴다 
	public void setFill(boolean fill) {
		if (fill)
			paint.setStyle(Paint.Style.FILL);
		else
			paint.setStyle(Paint.Style.STROKE);
	}

	// 색상을 설정. 인자는 정수형으로 명시되어 있지만 지정된 상수 및 변수값을 다 받을 수 있다. 
	public void setColor(int c) {
		paint.setColor(c);
	}

	// 외곽선의 넓이를 설정
	public void setStrokeWidth(float w) {
		paint.setStrokeWidth(w);
	}

	// 선을 그린다. 각각 x, y 값 두 개를 인자로 받는다 
	public void paintLine(float x1, float y1, float x2, float y2) {
		canvas.drawLine(x1, y1, x2, y2, paint);
	}

	// 사각형을 그린다. 출력위치 x, y 값과 넓이, 높이를 인자로 받는다
	public void paintRect(float x, float y, float width, float height) {
		canvas.drawRect(x, y, x + width, y + height, paint);
	}
	
	// 비트맵을 출력한다. 비트맵 객체와 출력지점 좌표 두 개를 인자로 받는다
	public void paintBitmap(Bitmap bitmap, float left, float top) {
		canvas.drawBitmap(bitmap, left, top, paint);
	}
	
	// 경로(패스)를 그린다. 패스 객체와 x, y, 넓이와 높이, 회전과 확대에 대한 값을 받는다
	// 사실 패스를 그리는 것 이외의 기능도 하는듯? 실제 작동여부를 파악 해봐야 겠다
	public void paintPath(Path path,float x, float y, float width, float height, float rotation, float scale) {
		canvas.save();	// 현 캔버스를 저장하고
		canvas.translate(x + width / 2, y + height / 2);	// 중심축 기준 이동
		canvas.rotate(rotation);	// 회전
		canvas.scale(scale, scale);	// 확대. 가로세로가 같은 비율로 확대 된다
		canvas.translate(-(width / 2), -(height / 2));	// 이동 작업의 완료
		canvas.drawPath(path, paint);	// 패스를 그린다
		canvas.restore();	// 원래의 상태로 되돌림
	}

	// 원을 그린다. 출력 위치(x, y)와 반지름 값을 인자로 받는다
	public void paintCircle(float x, float y, float radius) {
		canvas.drawCircle(x, y, radius, paint);
	}

	// 텍스트를 출력한다. 출력위치, 텍스트, 밑줄여부를 인자로 받는다
	public void paintText(float x, float y, String text, boolean underline) {
		paint.setUnderlineText(underline);
		canvas.drawText(text, x, y, paint);
	}

	// 객체를 그린다. 스크린 객체와 출력될 좌표(x, y), 회전각과 확대수치를 받는다
	public void paintObj(ScreenObj obj, float x, float y, float rotation,
			float scale) {
		canvas.save();	// 현재 캔버스를 저장
		// x, y 좌표의 처리는 화면 중심 기준이기 때문에, translate를 두 번 호출한다.
		// 어쩌면 회전연산을 중심 기준으로 하기 위함일 지도 모르겠다.
		canvas.translate(x + obj.getWidth() / 2, y + obj.getHeight() / 2);
		canvas.rotate(rotation);
		canvas.scale(scale, scale);
		canvas.translate(-(obj.getWidth() / 2), -(obj.getHeight() / 2));
		obj.paint(this);
		canvas.restore();	// 모든 작업이 끝나면 캔버스 복구
	}

	// 텍스트의 넓이를 측정, 반환
	public float getTextWidth(String txt) {
		return paint.measureText(txt);
	}

	// 텍스트의 경사를 측정
	public float getTextAsc() {
		return -paint.ascent();
	}

	public float getTextDesc() {
		return paint.descent();
	}

	// ?
	public float getTextLead() {
		return 0;
	}

	// 폰트의 크기를 반환
	public void setFontSize(float size) {
		paint.setTextSize(size);
	}
}