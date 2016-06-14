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

package org.mixare.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.mixare.Marker;
import org.mixare.MixContext;
import org.mixare.MixView;
import org.mixare.NavigationMarker;
import org.mixare.POIMarker;
import org.mixare.SocialMarker;

import android.location.Location;
import android.util.Log;

/**
 * DataHandler is the model which provides the Marker Objects.
 * 
 * DataHandler is also the Factory for new Marker objects.
 */
// 데이터 핸들러 클래스. 마커 오브젝트와 연동된다
public class DataHandler implements Serializable{
	
	// 완전한 정보를 가진 마커 리스트
	private List<Marker> markerList = new ArrayList<Marker>();
	
	// 마커들을 리스트에 추가
	public void addMarkers(List<Marker> markers) {
		// 추가 이전 리스트의 사이즈 로그 생성 
		Log.v(MixView.TAG, "Marker before: "+markerList.size());
		
		// 인자로 받은 마커들을 리스트에 추가한다(중복은 방지)
		for(Marker ma:markers) {
			if(!markerList.contains(ma))
				markerList.add(ma);
		}
		
		// 추가 이후 리스트의 사이즈 로그 생성
		Log.d(MixView.TAG, "Marker count: "+markerList.size());
	}
	
	// 마커 정렬. 기본 소트를 이용
	public void sortMarkerList() {
		Collections.sort(markerList); 
	}
	
	// 각 마커 위치의 거리 갱신
	public void updateDistances(Location location) {
		// 리스트에 있는 모든 마커를 갱신한다
		for(Marker ma: markerList) {
			float[] dist=new float[3];
			// 인자로 받은 곳과의 거리 계산
			Location.distanceBetween(ma.getLatitude(), ma.getLongitude(), location.getLatitude(), location.getLongitude(), dist);
			ma.setDistance(dist[0]);	// 계산된 값을 마커의 거리에 대입
		}
	}
	
	// 활성화 상태 갱신. 혼합된 컨텍스트를 인자로 받는다
	public void updateActivationStatus(MixContext mixContext) {
		// 클래스와 정수형 변수의 해쉬테이블 맵
		Hashtable<Class, Integer> map = new Hashtable<Class, Integer>();
		
		// 모든 마커에 적용
		for(Marker ma: markerList) {

			Class mClass = ma.getClass();
			// 맵의 클래스가 null 이 아닐 경우에 클래스를 대입. null 일 경우엔 1
			map.put(mClass, (map.get(mClass)!=null)?map.get(mClass)+1:1);
			
			// 최대 객체수보다 밑인지 판단
			boolean belowMax = (map.get(mClass) <= ma.getMaxObjects());
			// 데이터 소스가 선택 되었는지 판단

			boolean dataSourceSelected = mixContext.isDataSourceSelected(ma.getDatasource());

			//Log.i("setActive값 지정 ")
			// 판단된 boolean 값으로 상태를 지정한다(모두 참일 시에 활성)

			ma.setActive((belowMax && dataSourceSelected));
		}
	}
	
	// 위치가 변경되었을 경우
	public void onLocationChanged(Location location) {
		updateDistances(location);	// 거리를 갱신하고
		sortMarkerList();			// 마커 리스트를 정렬 
		for(Marker ma: markerList) {
			ma.update(location);	// 위치를 업데이트 해 준다
			// 나중엔 소셜 마커까지도!
		}
	}
	
	/**
	 * @deprecated Nobody should get direct access to the list
	 */
	public List getMarkerList() {
		return markerList;
	}
	
	/**
	 * @deprecated Nobody should get direct access to the list
	 */
	public void setMarkerList(List markerList) {
		this.markerList = markerList;
	}

	// 리스트 내의 마커 수를 리턴
	public int getMarkerCount() {
		return markerList.size();
	}
	
	// 리스트내의 인덱스에 인취하는 마커를 리턴
	public Marker getMarker(int index) {
		return markerList.get(index);
	}
}
