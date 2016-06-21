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
import java.util.Vector;

import org.mixare.data.DataHandler;
import org.mixare.data.DataSource.DATASOURCE;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// 리스트 액티비티를 확장하는 리스트 뷰 클래스
public class MixListView extends ListActivity {

	private static int list;	// 리스트의 종류(분류)

	// 벡터로 관리되는 항목들
	private Vector<SpannableString> listViewMenu;	// 리스트뷰 메뉴
	private Vector<String> selectedItemURL;			// 선택된 항목의 URL
	private Vector<String> dataSourceMenu;			// 데이터 소스 메뉴
	private Vector<Boolean> dataSourceChecked;		// 데이터 소스 체크 여부
	// 메인 컨텍스트와 데이터 뷰
	private MixContext mixContext;
	private DataView dataView;

	// 이전에 사용된 방식
	//private static String selectedDataSource = "Wikipedia";
	/*어떤 데이터 소스가 활성화 되었는지 체크하기 위함*/
	//	private int clickedDataSourceItem = 0;

	private ListItemAdapter adapter;	// 리스트 아이템 어댑터
	// 자체 주소로 사용될 커스텀 URL
	public static String customizedURL = "http://map.naver.com/search2/interestSpot.nhn";
	private static Context ctx;	// 컨텍스트
	private static String searchQuery = "";	// 검색 쿼리
	private static SpannableString underlinedTitle;	// 밑줄 쳐진 타이틀
	public static List<Marker> searchResultMarkers;	// 검색 결과 마커 리스트
	public static List<Marker> originalMarkerList;	// 오리지널 마커 리스트

	// 데이터 소스 메뉴를 리턴
	public Vector<String> getDataSourceMenu() {
		return dataSourceMenu;
	}


	// 데이터소스 체크여부를 리턴
	public Vector<Boolean> getDataSourceChecked() {
		return dataSourceChecked;
	}

	// 생성시 호출
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//		mixCtx = MixView.ctx;

		// 데이터 뷰와 컨텍스트를 할당
		dataView = MixView.dataView;
		ctx = this;
		mixContext = dataView.getContext();	// 메인 컨텍스트를 할당

		// 리스트의 분류에 따른 처리
		switch(list){
			case 1:	// 데이터 소스를 선택하는 리스트
				//TODO: this needs some cleanup
				// TODO: 2016-05-31  이거는 나중에 기본은 학교 정보를 받아 오도록 하고 카페,버스정류장,학식 같은 편의정보도 받아오도록하자.
				// 메뉴 타이틀을 설정
				dataSourceMenu = new Vector<String>();

				dataSourceMenu.add("[KHU] 학교 정보");
				dataSourceMenu.add("[KHU] 학교 식당");
				dataSourceMenu.add("버스 정류장");
				dataSourceMenu.add("카페");
				dataSourceMenu.add("편의점");
				dataSourceMenu.add("식당");
				dataSourceMenu.add("[SNS] 발자취");

				// 각 항목의 체크여부를 등록
				dataSourceChecked = new Vector<Boolean>();
				//dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.OSM));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.SCHOOL));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.SCHOOLRestaurant));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.BUSSTOP));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.CAFE));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.Convenience));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.Restaurant));
				dataSourceChecked.add(mixContext.isDataSourceSelected(DATASOURCE.SNS));

				// 리스트 어댑터를 생성하고 설정
				adapter = new ListItemAdapter(this);
				//adapter.colorSource(getDataSource());
				getListView().setTextFilterEnabled(true);
				setListAdapter(adapter);
				break;

			case 2:	// 데이터 항목들을 선택하는 리스트
				// 선택항목 URL 과 리스트 뷰의 메뉴 항목
				selectedItemURL = new Vector<String>();
				listViewMenu = new Vector<SpannableString>();
				// 데이터 핸들러
				DataHandler jLayer = dataView.getDataHandler();

				// 데이터 뷰가 얼어있고 마커의 수가 한개 이상 존재할 때
				if (dataView.isFrozen() && jLayer.getMarkerCount() > 0){
					selectedItemURL.add("search");	// 선택 항목의 URL 에 'search' 추가
				}

			/*모든 마커 항목들을 타이틀과 URL 벡터에 추가한다*/
				for (int i = 0; i < jLayer.getMarkerCount(); i++) {
					Marker ma = jLayer.getMarker(i);	// 데이터 핸들러로부터 마커를 읽는다
					// 마커가 활성화 상태일 때
					if(ma.isActive()) {
						if (ma.getURL()!=null) {
						/*웹사이트가 가능한 상태라면 타이틀에 밑줄을 친다*/
							String tagName= "";

							DATASOURCE tempDataSource = ma.getDatasource();

							if(tempDataSource == DATASOURCE.SCHOOL || tempDataSource == DATASOURCE.SCHOOLRestaurant)
								tagName = "[KHU] ";
							else if(tempDataSource == DATASOURCE.BUSSTOP)
								tagName = "[BUS] ";
							else if(tempDataSource == DATASOURCE.CAFE)
								tagName ="[카페] ";
							else if(tempDataSource == DATASOURCE.Convenience)
								tagName ="[편의점] ";
							else if(tempDataSource == DATASOURCE.Restaurant)
								tagName ="[식당] ";
							else
								tagName ="[SNS] ";



							underlinedTitle = new SpannableString("  "+String.valueOf((int) ma.getDistance()) + "m"  + "   |   " + tagName + ma.getTitle());

							//underlinedTitle.setSpan(new UnderlineSpan(), 0, underlinedTitle.length(), 0);
							// 리스트 뷰에 추가한다
							listViewMenu.add(underlinedTitle); // 리스트 뷰 메뉴

						} else {
							// 그 외의 경우엔 그대로 추가
							listViewMenu.add(new SpannableString(ma.getTitle()));
						}
						Log.i("마커링크", ma.getURL());
					/*타이틀이 일치하는 웹사이트를 등록*/
						if (ma.getURL()!=null)
							selectedItemURL.add(ma.getURL());
					/*특정한 타이틀에 사용가능한 URL이 없을 경우*/
						else
							selectedItemURL.add("");
					}
				}

				// 데이터 뷰가 얼어 있을 경우
				if (dataView.isFrozen()) {
					// 검색 알림 텍스트. 어떤 데이터 소스로부터 읽어왔는지 출력한다
					TextView searchNotificationTxt = new TextView(this);
					searchNotificationTxt.setVisibility(View.VISIBLE);
					searchNotificationTxt.setText(getString(DataView.SEARCH_ACTIVE_1)+" "+ mixContext.getDataSourcesStringList() + getString(DataView.SEARCH_ACTIVE_2));
					searchNotificationTxt.setWidth(MixView.dWindow.getWidth());

					searchNotificationTxt.setPadding(10, 2, 0, 0);
					searchNotificationTxt.setBackgroundColor(Color.WHITE);
					searchNotificationTxt.setTextColor(Color.BLACK);

					getListView().addHeaderView(searchNotificationTxt);

				}

				// 리스트 어탭터를 세팅한다
				setListAdapter(new ArrayAdapter<SpannableString>(this, android.R.layout.simple_list_item_1,listViewMenu));
				getListView().setTextFilterEnabled(true);
				break;

		}
	}

	// 인텐트를 다룸
	private void handleIntent(Intent intent) {
		// 검색 버튼을 눌렀을 때의 처리
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	// 새 인텐트 생성시
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	// 자체 검색 기능을 수행. 쿼리를 넘겨받는다
	private void doMixSearch(String query) {
		// 데이터 핸들러를 읽어옴
		DataHandler jLayer = dataView.getDataHandler();
		// 데이터 뷰가 얼어있지 않을 때, 마커 리스트를 읽어옴
		if (!dataView.isFrozen()) {
			originalMarkerList = jLayer.getMarkerList();
			MixMap.originalMarkerList = jLayer.getMarkerList();
		}
		originalMarkerList = jLayer.getMarkerList();
		searchResultMarkers = new ArrayList<Marker>();	// 검색 결과를 저장할 리스트
		setSearchQuery(query);	// 검색 쿼리를 지정

		selectedItemURL = new Vector<String>();	// 선택된 항목의 URL
		listViewMenu = new Vector<SpannableString>();	// 리스트 뷰의 메뉴

		// 데이터 핸들러의 마커로부터 검색 결과가 일치하는 것을 찾는다  
		for(int i = 0; i < jLayer.getMarkerCount();i++){
			Marker ma = jLayer.getMarker(i);

			// 조건에 맞는 마커를 찾으면
			if (ma.getTitle().toLowerCase().indexOf(searchQuery.toLowerCase()) != -1) {
				searchResultMarkers.add(ma);	// 검색 결과에 추가
				listViewMenu.add(new SpannableString(ma.getTitle()));	// 메뉴에도 추가
				/*타이틀이 일치하는 웹사이트를 등록*/
				if (ma.getURL() != null)
					selectedItemURL.add(ma.getURL());
				/*특정한 타이틀에 사용가능한 URL이 없을 경우*/
				else
					selectedItemURL.add("");
			}
		}
		// 결과 리스트 뷰에 등록된 항목이 없을 경우
		if (listViewMenu.size() == 0) {
			Toast.makeText( this, getString(DataView.SEARCH_FAILED_NOTIFICATION), Toast.LENGTH_LONG ).show();
		}
		else {	// 등록된 항목이 있다면 
			jLayer.setMarkerList(searchResultMarkers);	// 결과 리스트를 등록
			dataView.setFrozen(true);	// 데이터뷰를 얼리고 리스트를 세팅 후 출력
			setList(2);
			finish();
			Intent intent1 = new Intent(this, MixListView.class);
			startActivityForResult(intent1, 42);
		}
	}

	// 리스트의 항목이 클릭되었을 경우
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		switch(list){	// 리스트의 종류에 따른 처리
		/*Data Sources*/
			case 1:	// 데이터 소스는 체크박스로 사용여부를 관리한다
				//clickOnDataSource(position);
				CheckBox cb = (CheckBox) v.findViewById(R.id.list_checkbox);
				cb.toggle();	// 누를시 마다 토글
				break;

		/*List View*/
			case 2:	// 리스트 뷰의 클릭 처리
				clickOnListView(position);
				break;
		}

	}

	// 리스트 뷰를 클릭 했을 경우

	// TODO: 2016-06-02 요것은 리스트 뷰 클릭했을때 생성 되는 부분 

	public void clickOnListView(int position){
		String selectedURL = position < selectedItemURL.size() ? selectedItemURL.get(position) : null;
		/*이 항목에 가능한 웹사이트가 없을 경우*/
		if (selectedURL == null || selectedURL.length() <= 0)
			Toast.makeText( this, getString(DataView.NO_WEBINFO_AVAILABLE), Toast.LENGTH_LONG ).show();
		else if("search".equals(selectedURL)){
			dataView.setFrozen(false);	// 데11이터 뷰를 얼리고 핸들러로부터 오리지널 마커 리스트를 읽어옴
			dataView.getDataHandler().setMarkerList(originalMarkerList);
			setList(2);	// 리스트에 결과를 할당하고 액티비티를 호출한다
			finish();
			Intent intent1 = new Intent(this, MixListView.class);
			startActivityForResult(intent1, 42);
		}
		else {	// 가능한 웹 페이지가 있을 경우, 파싱을 통해 웹페이지를 불러온다
			try {
				if (selectedURL.startsWith("webpage")) {
					String newUrl = MixUtils.parseAction(selectedURL);
					dataView.getContext().loadWebPage(newUrl, this);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 컨텍스트 메뉴 생성시
	public static void createContextMenu(ImageView icon) {
		// 리스너 등록
		icon.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				int index=0;
				switch(ListItemAdapter.itemPosition){	// 아이템 포지션에 따라 내용 추가

					case 0:
						break;
					case 1:
						break;
					case 2:
						break;
					case 3:
						menu.setHeaderTitle("OpenStreetMap Menu");
						menu.add(index, index, index, "We are working on it...");
						break;
					case 4:	// 커스텀 URL을 입력가능한 얼럿 다이얼로그를 생성한다
						break;

					case 5:
						break;
					case 6:
						break;





				}
			}
		});

	}

	// 리스트의 각 항목(데이터소스들)을 클릭했을 경우
	public void clickOnDataSource(int position){
		// 얼려있는 데이터뷰를 해동시킨다
		if(dataView.isFrozen())
			dataView.setFrozen(false);

		// 각 포지션에 따라 데이터소스 사용여부를 토글한다
		switch(position){

			case 0:
				mixContext.toogleDataSource(DATASOURCE.SCHOOL);
				break;
			case 1:
				mixContext.toogleDataSource(DATASOURCE.SCHOOLRestaurant);
				break;
			case 2:
				mixContext.toogleDataSource(DATASOURCE.BUSSTOP);
				break;
			case 3:
				mixContext.toogleDataSource(DATASOURCE.CAFE);
				break;
			case 4:
				mixContext.toogleDataSource(DATASOURCE.Convenience);
				break;
			case 5:
				mixContext.toogleDataSource(DATASOURCE.Restaurant);
				break;
			case 6:
				mixContext.toogleDataSource(DATASOURCE.SNS);
				break;
			//	case 6:
			//		mixContext.toogleDataSource(DATASOURCE.ARRIVEBUS);
			//		break;

		}
	}


	// 옵션 메뉴 생성시
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int base = Menu.FIRST;

		// 맵뷰와 카메라 뷰 버튼을 생성

		/*메뉴 항목 정의*/
		MenuItem item1 = menu.add(base, base, base, getString(DataView.MENU_ITEM_3));
		MenuItem item2 = menu.add(base, base+1, base+1, getString(DataView.MENU_CAM_MODE));

		/*메뉴 항목의 아이콘 할당*/
		item1.setIcon(android.R.drawable.ic_menu_mapmode);
		item2.setIcon(android.R.drawable.ic_menu_camera);

		return true;
	}

	// 각 옵션 메뉴가 선택되었을 경우
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		/*Map View*/
			case 1:
				createMixMap();	// 맵 뷰 생성
				finish();
				break;
			
		/*back to Camera View*/
			case 2:
				finish();
				break;
		}
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case 1:
				break;
			case 2:
				break;
		}
		return false;
	}

	// 맵 뷰를 생성하여 호출한다
	public void createMixMap(){
		Intent intent2 = new Intent(MixListView.this, MixMap.class);
		startActivityForResult(intent2, 20);
	}


	// 사용하게 될 리스트 번호 할당
	public static void setList(int l){
		list = l;
	}

	// 검색 쿼리 리턴
	public static String getSearchQuery(){
		return searchQuery;
	}

	// 검색 쿼리 할당
	public static void setSearchQuery(String query){
		searchQuery = query;
	}
}

// 기본 어댑터를 확장하는 리스트 항목 어댑터
class ListItemAdapter extends BaseAdapter {

	private MixListView mixListView;	// 리스트 뷰

	private LayoutInflater myInflater;	// 레이아웃을 전개 할 인플레이터
	static ViewHolder holder;	// 뷰의 홀더
	// 배경, 텍스트, 설명의 컬러
	private int[] textcolors = new int[] {Color.WHITE,Color.WHITE,Color.WHITE,Color.WHITE,Color.WHITE};
	private int[] bgcolors = new int[] {0,0,0,0,0};
	private int[] descriptioncolors = new int[] {Color.GRAY,Color.GRAY,Color.GRAY,Color.GRAY,Color.GRAY};

	public static boolean icon_clicked = false;	// 아이콘이 클릭되었는지 여부

	public static int itemPosition = 0;	// 각 항목들의 위치

	// 생성자. 레이아웃 전개
	public ListItemAdapter(MixListView mixListView) {
		this.mixListView = mixListView;
		myInflater = LayoutInflater.from(mixListView);
	}

	// 현재의 뷰를 리턴
	// TODO: 2016-06-02 이거 데이터 선택하는거 만드는 액티비티 
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		itemPosition = position;	// 포지션을 저장

		// 전환 뷰가 없을 경우 리스트 뷰의 레이아웃과 데이터를 할당
		if (convertView == null) {
			// 메인 레이아웃을 전개하여 할당 
			convertView = myInflater.inflate(R.layout.main, null);

			// 홀더를 이용해 각 컴포넌트를 할당
			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.list_text);
			holder.description = (TextView) convertView.findViewById(R.id.description_text);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.list_checkbox);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);

			convertView.setTag(holder);
		}
		else{
			// getTag()를 이용해 컨버트 뷰로부터의 홀더를 할당
			holder = (ViewHolder) convertView.getTag();
		}

		// 아이콘을 클릭 가능하게 설정하고
		holder.icon.setPadding(20, 8, 20, 8);
		holder.icon.setClickable(true);
		// 리스너를 등록
		holder.icon.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				icon_clicked = true;
				itemPosition = position;

				return false;
			}
		});
		MixListView.createContextMenu(holder.icon);	// 컨텍스트 메뉴 생성

		// 4번째(자체 URL)을 제외한 아이콘은 감춘다
		if(position!=4){
			holder.icon.setVisibility(View.INVISIBLE);
		}

		// 각 항목들의 체크박스의 동작등을 등록한다
		holder.checkbox.setChecked(mixListView.getDataSourceChecked().get(position));

		holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
										 boolean isChecked) {
				mixListView.clickOnDataSource(position);    // 데이터소스의 클릭처리
			}

		});

		// 텍스트와 설명의 위치 조절(여백값 설정)
		holder.text.setPadding(20, 8, 0, 0);
		holder.description.setPadding(20, 40, 0, 0);

		// 텍스트와 설명의 내용 할당
		holder.text.setText(mixListView.getDataSourceMenu().get(position));

		// 색 설정
		int colorPos = position % bgcolors.length;
		convertView.setBackgroundColor(bgcolors[colorPos]);
		holder.text.setTextColor(textcolors[colorPos]);
		holder.description.setTextColor(descriptioncolors[colorPos]);

		return convertView;	// 전환된 뷰를 리턴
	}

	// 색 변경
	public void changeColor(int index, int bgcolor, int textcolor){
		if (index < bgcolors.length) {
			bgcolors[index]=bgcolor;
			textcolors[index]= textcolor;
		}
		else
			Log.d("Color Error", "too large index");
	}

	// 각 데이터 소스에 따른 색 설정
	public void colorSource(String source){
		for (int i = 0; i < bgcolors.length; i++) {
			bgcolors[i]=0;
			textcolors[i]=Color.WHITE;
		}

		//if (source.equals("CAFE"))
		//	changeColor(3, Color.RED, Color.RED);
		//else if (source.equals("OwnURL"))
		//	changeColor(4, Color.WHITE, Color.DKGRAY);

	}

	// 데이터 소스 메뉴의 수를 리턴
	@Override
	public int getCount() {
		return mixListView.getDataSourceMenu().size();
	}

	// 특정 포지션의 항목 리턴
	@Override
	public Object getItem(int position) {
		return this;
	}

	// 특정 포지션 항목의 ID를 리턴
	@Override
	public long getItemId(int position) {
		return position;	// 일단 ID는 포지션 값과 똑같게 한다
	}

	// 뷰 홀더. 텍스트, 설명, 체크박스, 아이콘 정보를 가진다
	private class ViewHolder {
		TextView text;
		TextView description;
		CheckBox checkbox;
		ImageView icon;
	}
}
