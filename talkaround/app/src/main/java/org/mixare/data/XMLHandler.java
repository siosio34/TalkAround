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

import java.util.ArrayList;
import java.util.List;

import org.mixare.Marker;
import org.mixare.MixView;
import org.mixare.NavigationMarker;
import org.mixare.reality.PhysicalPlace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

/**
 * @author hannes
 *
 */
// XML 파일을 다루는 클래스
public class XMLHandler extends DataHandler {

	// OSM(OpenStreetMap) 처리
	private List<Marker> processOSM(Element root) {

		// 마커들과 노드들
    	List<Marker> markers = new ArrayList<Marker>();
        NodeList nodes = root.getElementsByTagName("node");
        
        // 각 노드들에 대한 처리
        for (int i =0; i< nodes.getLength(); i++) {
        	// 각 노드를 할당하고, 속성과 태그를 읽어옴
        	Node node = nodes.item(i);
        	NamedNodeMap att = node.getAttributes();
        	NodeList tags = node.getChildNodes();
        	
        	// 태그에 따른 처리
        	for(int j=0;j<tags.getLength();j++) {
        		// 각 태그를 할당
        		Node tag = tags.item(j);

        		// 텍스트 노드가 아니라면 
        		if(tag.getNodeType()!=Node.TEXT_NODE) {
        			// 일단 키값을 읽어오고
	        		String key = tag.getAttributes().getNamedItem("k").getNodeValue();

	        		// 키 값이 이름일 경우
	        		if (key.equals("name")) {
	        			// 명칭과 위도, 경도
	        			String name = tag.getAttributes().getNamedItem("v").getNodeValue();
	                	double lat = Double.valueOf(att.getNamedItem("lat").getNodeValue());
	                	double lon = Double.valueOf(att.getNamedItem("lon").getNodeValue());
	        			
	                	// 로그 출력
	                	Log.v(MixView.TAG,"OSM Node: "+name+" lat "+lat+" lon "+lon+"\n");


	                	Marker ma = new NavigationMarker(
	        				name, 
	        				lat, 
	        				lon, 
	        				0,
	        				// OSM의 데이터를 이용한다
	        				"http://www.openstreetmap.org/?node="+att.getNamedItem("id").getNodeValue(), 
	        				DataSource.DATASOURCE.CAFE);
	        			markers.add(ma);
	                	//skip to next node
	        			continue;
	        		}
        		}
        	}
        }
        return markers;
	}
	
	// OSM경계박스를 읽어옴. 위도와 경도, 반경을 인자로 받음
	public static String getOSMBoundingBox(double lat, double lon, double radius) {
		// 리턴하게될 스트링값
		String bbox = "[bbox=";
		// 위치를 연산해 넣을 물리적 장소 객체
		PhysicalPlace lb = new PhysicalPlace(); // left bottom
		PhysicalPlace rt = new PhysicalPlace(); // right top
		
		// 적절한 값을 연산해 물리적 객체에 넣음. 고도에 따라 좌상, 우하 지점을 설정 
		PhysicalPlace.calcDestination(lat, lon, 225, radius*1414, lb); // 1414: sqrt(2)*1000
		PhysicalPlace.calcDestination(lat, lon, 45, radius*1414, rt);
		// 각 위도와 경도를 결과 스트링에 삽입
		bbox+=lb.getLongitude()+","+lb.getLatitude()+","+rt.getLongitude()+","+rt.getLatitude()+"]";
		
		// 결과를 리턴
		return bbox;

		//return "[bbox=16.365,48.193,16.374,48.199]";
	}
	
	// 도큐먼트를 읽음. OSM데이터를 읽기 위함
	public List<Marker> load(Document doc) {
        Element root = doc.getDocumentElement();
        
        // If the root tag is called "osm" we got an 
        // openstreetmap .osm xml document
        if ("osm".equals(root.getTagName()))
        	return processOSM(root);
        return null;
	}
}
