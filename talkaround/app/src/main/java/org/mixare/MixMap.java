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

import java.util.ArrayList;
import java.util.List;

import org.mixare.data.DataHandler;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

// 구글맵을 띄우는 맵 액티비티를 확장하는 클래스
public class MixMap extends MapActivity implements OnTouchListener{

	// 각 오버레이들을 저장할 변수
	private static List<Overlay> mapOverlays;
	private Drawable drawable;

	// 오버레이에 연동될 마커들과 데이터 뷰, 지오포인트
	private static List<Marker> markerList;
	private static DataView dataView;
	private static GeoPoint startPoint;

	// 맵뷰와 메인 컨텍스트
	private MixContext mixContext;
	private MapView mapView;

	// 맵과 스스로를 가리킬 컨텍스트, 알림텍스트와 오리지널 마커들
	static MixMap map;
	private static Context thisContext;
	private static TextView searchNotificationTxt;
	public static List<Marker> originalMarkerList;

	// 경로가 표시되어 있는지 리턴
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	// 맵 생성시
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 데이터 뷰와 메인 컨텍스트를 읽어옮
		dataView = MixView.dataView;
		mixContext = dataView.getContext();

		// 읽어온 데이터를 마커리스트에 연결
		setMarkerList(dataView.getDataHandler().getMarkerList());

		// 맵 컨텍스트를 할당
		map = this;
		setMapContext(this);
		// 구글 API키 지정. 각자 개발자 스스로 인증받은 키값을 입력해야 구글맵이 작동한다
		mapView = new MapView(this, "AIzaSyBLJEoQYktBnc2JbvO8VbMU7-zfVZevBrw");

		// 맵뷰의 옵션 설정. 줌 컨트롤, 클릭 가능하고, 위성표시 후 활성화
		mapView.setBuiltInZoomControls(true);
		mapView.setClickable(true);
		mapView.setSatellite(true);
		mapView.setEnabled(true);

		// 컨텍스트 뷰를 맵 뷰로 설정 
		this.setContentView(mapView);
		// 시작점을 지정하고 오버레이를 생성한다
		setStartPoint();
		createOverlay();

		// 데이터 뷰가 얼어있을 경우
		if (dataView.isFrozen()){
			// 각 데이터를 소스로부터 읽어오고 있음을 알리는 알림텍스트 설정
			searchNotificationTxt = new TextView(this);
			searchNotificationTxt.setWidth(MixView.dWindow.getWidth());
			searchNotificationTxt.setPadding(10, 2, 0, 0);
			searchNotificationTxt.setText(getString(DataView.SEARCH_ACTIVE_1)+" "+ mixContext.getDataSourcesStringList() + getString(DataView.SEARCH_ACTIVE_2));
			searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
			searchNotificationTxt.setTextColor(Color.WHITE);

			// 터치리스너를 등록하고 컨텐트뷰에 추가(표시)
			searchNotificationTxt.setOnTouchListener(this);
			addContentView(searchNotificationTxt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}

	// 시작점을 설정
	public void setStartPoint() {
		// 현재의 위치를 읽어옴
		Location location = mixContext.getCurrentLocation();

		//Location location = null;
		//location.setLatitude(37.2398864);
		//location.setLongitude(127.0813631);
		MapController controller;	// 맵을 제어하기위한 컨트롤러

		// 현재 위치의 위도와 경도를 읽어옴
		double latitude = location.getLatitude()*1E6;
		double longitude = location.getLongitude()*1E6;

		Toast.makeText(this, latitude + ", " + longitude, Toast.LENGTH_LONG).show();

		// 맵 뷰의 컨트롤러를 읽고, 시작점을 지정한다. 
		controller = mapView.getController();
		startPoint = new GeoPoint((int)latitude, (int)longitude);
		// 설정된 시작점을 중앙으로, 줌 수치를 15로 설정한다 
		controller.setCenter(startPoint);
		controller.setZoom(15);
	}

	// 오버레이 생성
	public void createOverlay(){
		// 맵뷰의 오버레이들을 읽어온다 
		mapOverlays = mapView.getOverlays();
		OverlayItem item;	// 오버레이 항목
		// 생성된 아이콘으로 새로운 오버레이를 만든다
		drawable = this.getResources().getDrawable(R.drawable.icon_map);
		MixOverlay mixOverlay = new MixOverlay(this, drawable);

		// 마커의 수 만큼 오버레이 생성
		for(Marker marker:markerList) {
			// 마커가 활성화 되었을 경우
			if(marker.isActive()) {
				// 지오포인트 형태로 바꾸기 위해 읽어온 위도, 경도값에 IE6를 곱한다 
				GeoPoint point = new GeoPoint((int)(marker.getLatitude()*1E6), (int)(marker.getLongitude()*1E6));
				// 새로 설정된 포인트로 새 오버레이 항목 생성 후 추가
				item = new OverlayItem(point, "", "");
				mixOverlay.addOverlay(item);
			}
		}
		//Solved issue 39: only one overlay with all marker instead of one overlay for each marker
		mapOverlays.add(mixOverlay);	// 오버레이 항목들 추가

		// 위와 같은 방식으로 현재 나의 위치를 나타내는 오버레이 추가
		MixOverlay myOverlay;
		drawable = this.getResources().getDrawable(R.drawable.loc_icon);
		myOverlay = new MixOverlay(this, drawable);

		item = new OverlayItem(startPoint, "Your Position", "");
		myOverlay.addOverlay(item);
		mapOverlays.add(myOverlay);
	}

	// 옵션메뉴 생성시
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// 맵 뷰에서의 메뉴를 설정하고
		int base = Menu.FIRST;
		/*define the first*/
		MenuItem item1 = menu.add(base, base, base, getString(DataView.MAP_MENU_NORMAL_MODE));
		MenuItem item2 = menu.add(base, base+1, base+1, getString(DataView.MAP_MENU_SATELLITE_MODE));
		MenuItem item3 = menu.add(base, base+2, base+2, getString(DataView.MAP_MY_LOCATION));
		MenuItem item4 = menu.add(base, base+3, base+3, getString(DataView.MENU_ITEM_2));
		MenuItem item5 = menu.add(base, base+4, base+4, getString(DataView.MENU_CAM_MODE));

		// 아이콘을 할당한다
		/*assign icons to the menu items*/
		item1.setIcon(android.R.drawable.ic_menu_gallery);
		item2.setIcon(android.R.drawable.ic_menu_mapmode);
		item3.setIcon(android.R.drawable.ic_menu_mylocation);
		item4.setIcon(android.R.drawable.ic_menu_view);
		item5.setIcon(android.R.drawable.ic_menu_camera);

		return true;
	}

	// 옵션 메뉴 선택시 
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		/*street View*/
		case 1:
			mapView.setSatellite(false);	// 위성 사용 않음
			break;
			
		/*Satellite View*/
		case 2:
			mapView.setSatellite(true);	// 위성 사용
			break;
		
		/*go to users location*/
		case 3:
			setStartPoint();	// 스타트 포인트로 옮김
			break;
		
		/*List View*/
		case 4:
			createListView();	// 리스트뷰 생성, 맵뷰 종료
			finish();
			break;
		
		/*back to Camera View*/
		case 5:
			finish();
			break;
		}
		return true;
	}

	// 리스트뷰 생성
	public void createListView(){
		MixListView.setList(2);
		// 데이터 뷰에 마커가 하나라도 있을 경우 
		if (dataView.getDataHandler().getMarkerCount() > 0) {
			// 리스트 뷰 액티비티를 호출한다
			Intent intent1 = new Intent(MixMap.this, MixListView.class);
			startActivityForResult(intent1, 42);
		}
		// 리스트가 비어있을 경우 토스트 출력
		else{
			Toast.makeText( this, DataView.EMPTY_LIST_STRING_ID, Toast.LENGTH_LONG ).show();
		}
	}

//	public static ArrayList<Marker> getMarkerList(){
//		return markerList;
//	}

	// 마커 리스트를 할당, 대체
	public void setMarkerList(List<Marker> maList){
		markerList = maList;
	}

	// 데이터 뷰를 리턴
	public DataView getDataView(){
		return dataView;
	}

//	public static void setDataView(DataView view){
//		dataView= view;
//	}

//	public static void setMixContext(MixContext context){
//		ctx= context;
//	}
//
//	public static MixContext getMixContext(){
//		return ctx;
//	}

	// 오버레이 리스트를 리턴
	public List<Overlay> getMapOverlayList(){
		return mapOverlays;
	}

	// 맵 컨텍스트를 설정
	public void setMapContext(Context context){
		thisContext= context;
	}

	// 맵 컨텍스트를 리턴
	public Context getMapContext(){
		return thisContext;
	}

	// 현재의 위치를 출력한다는 메세지
	public void startPointMsg(){
		Toast.makeText(getMapContext(), DataView.MAP_CURRENT_LOCATION_CLICK, Toast.LENGTH_LONG).show();
	}

	// 인텐트를 다룬다
	private void handleIntent(Intent intent) {
		// 검색 버튼을 눌렀을 경우
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);	// 읽어들인 쿼리로 검색을 수행
		}
	}

	// 새로운 인텐트를 세팅 할때 호출. setIntent()와 handleIntent()를 호출한다
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	// 검색 작업을 수행
	private void doMixSearch(String query) {
		// 데이터 핸들러를 읽어올 임시변수
		DataHandler jLayer = dataView.getDataHandler();

		// 데이터 뷰가 얼어있지 않다면
		if (!dataView.isFrozen()) {
			// 데이터 핸들러로부터 마커 리스트를 읽어 오리지널 마커 리스트에 할당 
			originalMarkerList = jLayer.getMarkerList();
			MixListView.originalMarkerList = jLayer.getMarkerList();
		}
		markerList = new ArrayList<Marker>();	// 마커 리스트 초기화

		// 마커 리스트에 데이터 핸들러로부터 검색된 마커들을 할당
		for(int i = 0; i < jLayer.getMarkerCount(); i++) {
			Marker ma = jLayer.getMarker(i);

			// 쿼리의 문자열과 비교
			if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase())!=-1){
				markerList.add(ma);
			}
		}
		if(markerList.size()==0){	// 할당된 마커가 없을 때 토스트 생성
			Toast.makeText( this, getString(DataView.SEARCH_FAILED_NOTIFICATION), Toast.LENGTH_LONG ).show();
		}
		else{	// 검색에 성공했을 경우
			// 완성된 리스트를 임시변수에 할당하고 데이터뷰를 얼린다
			jLayer.setMarkerList(markerList);
			dataView.setFrozen(true);

			// 다시 이전 액티비티(MixMap)로 전환
			finish();
			Intent intent1 = new Intent(this, MixMap.class);
			startActivityForResult(intent1, 42);
		}
	}

	// * 이 부분에 대해서는 정확한 파악이 필요. 어디에 어떤식으로 사용되는거지?
	// 터치 발생시
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// 데이터 뷰를 얼리고 오리지널 마커 리스트를 할당한다(?)
		dataView.setFrozen(false);
		dataView.getDataHandler().setMarkerList(originalMarkerList);

		// 알림 텍스트를 감추고 이전 액티비티로 돌아간다
		searchNotificationTxt.setVisibility(View.INVISIBLE);
		searchNotificationTxt = null;
		finish();
		Intent intent1 = new Intent(this, MixMap.class);
		startActivityForResult(intent1, 42);

		return false;
	}

}

// 맵 뷰 위에 표시될 오버레이 클래스. 항목 오버레이를 확장
class MixOverlay extends ItemizedOverlay<OverlayItem> {
	// 항목 오버레이의 리스트와 표시될 맵
	private ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	private MixMap mixMap;

	// 생성자. 표시될 맵과 드로블 객체를 인자로 받는다
	public MixOverlay(MixMap mixMap, Drawable marker){
		super (boundCenterBottom(marker));
		// 버그 방지를 위해 populate()를 호출해 준다
		// * 참고 > http://code.google.com/p/android/issues/detail?id=2035
		populate();
		this.mixMap = mixMap;
	}

	// 오버레이 아이템을 생성. 리스트에서 i 번째 아이템을 얻어온다  
	@Override
	protected OverlayItem createItem(int i) {
		return overlayItems.get(i);
	}

	// 오버레이 리스트의 사이즈를 리턴
	@Override
	public int size() {
		return overlayItems.size();
	}

	// 탭된 오버레이에 대한 처리
	@Override
	protected boolean onTap(int index){
		// 사이즈가 1 일 경우엔 오버레이가 현 위치 표식 뿐일 것이므로
		if (size() == 1)
			mixMap.startPointMsg();	// 시작점에 대한 메세지 출력
		else if (mixMap.getDataView().getDataHandler().getMarker(index).getURL() !=  null) {
			// 이외의 경우에는 마커에 할당된 url 을 읽어온다 
			String url = mixMap.getDataView().getDataHandler().getMarker(index).getURL();
			Log.d("MapView", "open url: "+url);	// url 에 대한 로그 생성
			try {
				// url 이 웹페이지라면
				if (url != null && url.startsWith("webpage")) {
					// 파싱 후 웹 페이지를 불러와 보여준다
					String newUrl = MixUtils.parseAction(url);
					mixMap.getDataView().getContext().loadWebPage(newUrl, mixMap.getMapContext());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	// 오버레이 항목 추가
	public void addOverlay(OverlayItem overlay) {
		overlayItems.add(overlay);
		populate();
	}
}

