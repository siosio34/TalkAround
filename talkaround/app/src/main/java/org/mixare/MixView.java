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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.R.drawable;
import org.mixare.data.DataHandler;
import org.mixare.data.DataSource;
import org.mixare.gui.PaintScreen;
import org.mixare.reality.PhysicalPlace;
import org.mixare.render.Matrix;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

// 메인에 보여지게 될 믹스뷰(액티비티) 클래스
public class MixView extends Activity implements SensorEventListener,LocationListener, OnTouchListener{


	//public NavigatorV2 navi;

	// TODO: 2016-06-01
	/* 내가 만든 변수들*/
	public Navi2 navi;

	int threadnum;


	private Sensor orientationSensor;
	private SensorManager sensorMgr_ori;
	private TextView oriTV = null;
	private TextView accTV =null;

	public double mapLat=0;
	public double  mapLog=0;
	public  static double gpsLat=0;//gps에서 수시로 받아오는 값
	public  static double gpsLong=0;
	public boolean onNavigate=true;

	Thread tt1;
	// 카메라 스크린과 증강된 뷰
	private CameraSurface camScreen;
	private AugmentedView augScreen;

	private boolean isInited;	// 초기 세팅이 되어 있는가
	private MixContext mixContext;	// 메인 컨텍스트
	static PaintScreen dWindow;		// 스크린 윈도우
	static DataView dataView;		// 데이터 뷰
	private Thread downloadThread;	// 마커의 내용을 다운로드 할 스레드

	// 연산에 사용될 임시 변수들
	private float RTmp[] = new float[9];
	private float Rot[] = new float[9];
	private float I[] = new float[9];
	private float grav[] = new float[3];
	private float mag[] = new float[3];

	// 센서와 위치 정보를 읽기 위함
	private SensorManager sensorMgr;
	private List<Sensor> sensors;
	private Sensor sensorGrav, sensorMag;
	private LocationManager locationMgr;
	private boolean isGpsEnabled;

	// 연산에 사용될 매트릭스 객체들
	private int rHistIdx = 0;
	private Matrix tempR = new Matrix();
	private Matrix finalR = new Matrix();
	private Matrix smoothR = new Matrix();
	private Matrix histR[] = new Matrix[60];
	private Matrix m1 = new Matrix();
	private Matrix m2 = new Matrix();
	private Matrix m3 = new Matrix();
	private Matrix m4 = new Matrix();

	private SeekBar myZoomBar;	// 줌 배율을 설정하기 위함
	private WakeLock mWakeLock;	// 화면이 점멸되지 않게 하기 위함

	private boolean fError;	// 에러 여부


	// 나침반 에러
	private int compassErrorDisplayed = 0;

	// 줌 레벨과 프로그레스 수치
	private String zoomLevel;
	private int zoomProgress;

	// 노티피케이션 텍스트 뷰
	private TextView searchNotificationTxt;

	// 로그에 쓰일 태그
	public static final String TAG = "Mixare";


	// 리스트 뷰에 쓰일, JSON으로부터 읽어온 타이틀과 URL을 저장할 벡터들 
//	private Vector<String> listDataVector;
//	private Vector<String> listURL;

	// 내부 저장공간에 저장될 프레퍼런스에 쓰일 이름
	public static final String PREFS_NAME = "MyPrefsFileForMenuItems";

	// GPS 사용이 가능한지 여부를 리턴
	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}

	// 줌 바가 보이는지 리턴
	public boolean isZoombarVisible() {
		return myZoomBar != null && myZoomBar.getVisibility() == View.VISIBLE;
	}

	// 줌 레벨을 리턴
	public String getZoomLevel() {
		return zoomLevel;
	}

	// 줌 프로그레스 수치를 리턴
	public int getZoomProgress() {
		return zoomProgress;
	}

	// 에러 처리 메소드
	public void doError(Exception ex1) {
		if (!fError) {
			fError = true;	// 에러 플래그를 true

			setErrorDialog();	// 에러 다이얼로그 호출

			ex1.printStackTrace();	// 에러 내용을 출력
			try {
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}

		try {
			augScreen.invalidate();	// 스크린의 갱신을 시도
		} catch (Exception ignore) {
		}
	}

	// 에러 발생시
	public void killOnError() throws Exception {
		if (fError)
			throw new Exception();
	}

	// 리페인트. 데이터 뷰와 페인트 스크린 객체를 새로 생성한다
	public void repaint() {
		dataView = new DataView(mixContext);
		dWindow = new PaintScreen();
		setZoomLevel();	// 줌 레벨도 재설정
	}

	// 에러 다이얼로그
	public void setErrorDialog(){
		// 얼럿 다이얼로그의 빌더 생성
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// 빌더에 메세지 등을 세팅
		builder.setMessage(getString(DataView.CONNECTION_ERROR_DIALOG_TEXT));
		builder.setCancelable(false);

		// 각 버튼의 처리
		/*Retry*/
		builder.setPositiveButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				fError=false;
				//TODO improve
				try {
					repaint();	// 리페인트 호출
				}
				catch(Exception ex){	// 실패시 에러 처리
					doError(ex);
				}
			}
		});
		/*Open settings*/
		builder.setNeutralButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON2, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// 무선 설정 인텐트를 호출
				Intent intent1 = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
				startActivityForResult(intent1, 42);
			}
		});
		/*Close application*/
		builder.setNegativeButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON3, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				System.exit(0);	// 어플리케이션 강제 종료
			}
		});
		AlertDialog alert = builder.create();
		alert.show();	// 얼럿 다이얼로그 호출
	}

	// 뷰 생성시
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// 데이터 소스로부터 아이콘 생성
		DataSource.createIcons(getResources());

		try {

			handleIntent(getIntent());	// 인텐트 제어

			// 전원관리자
			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			// 화면이 꺼지지 않게 하기위한 웨이크 락
			this.mWakeLock = pm.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
			// 위치관리자의 설정
			locationMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			// 위치정보 업데이트 설정. 2번째 인자 시간(1/1000s), 3번째 인자 거리(m)에 따라 갱신한다
			locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,3, this);

			//orientation sensor 설정
			sensorMgr_ori=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
			orientationSensor = sensorMgr_ori.getDefaultSensor(Sensor.TYPE_ORIENTATION);

			killOnError();	// 에러 여부를 체크한다
			requestWindowFeature(Window.FEATURE_NO_TITLE);	// 타이틀 바가 없는 윈도우 형태로

			/*내부 메모리에 저장된 프레퍼런스를 불러온다*/
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();	// 변경사항을 기록할 에디터

			// 줌 바의 생성과 설정
			myZoomBar = new SeekBar(this);
			myZoomBar.setVisibility(View.INVISIBLE);
			myZoomBar.setMax(100);
			// 프레퍼런스로부터 줌 레벨을 읽는다. 디폴트 값은 65
			myZoomBar.setProgress(settings.getInt("zoomLevel", 65));
			// 체인지 리스너를 등록하고
			myZoomBar.setOnSeekBarChangeListener(myZoomBarOnSeekBarChangeListener);
			myZoomBar.setVisibility(View.INVISIBLE);	// 갱신

			// 프레임 레이아웃을 사용한다
			FrameLayout frameLayout = new FrameLayout(this);

			// 최소 넓이를 설정하고 줌 바를 등록, 여백을 설정한다
			frameLayout.setMinimumWidth(3000);
			frameLayout.addView(myZoomBar);
			frameLayout.setPadding(10, 0, 10, 10);

			// 카메라 스크린과 증강 스크린을 생성하고
			camScreen = new CameraSurface(this);
			augScreen = new AugmentedView(this);
			setContentView(camScreen);	// 카메라 스크린을 컨텐트 뷰로 등록 후

			// 증강 스크린을 덧씌워 준다
			addContentView(augScreen, new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			// 최종적으로 더해지는 것은 위에서 설정한 프레임 레이아웃.
			// 레이아웃 파라메터를 통해 표면의 가장 위이자 공간의 가장 아래에 추가 된다 
			addContentView(frameLayout, new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM));

			// 초기 세팅된 상태가 아니라면
			if (!isInited) {
				mixContext = new MixContext(this);	// 컨텍스트 생성
				// 다운로드관리자 등록
				mixContext.downloadManager = new DownloadManager(mixContext);

				// 페인트 스크린과 데이터 뷰의 생성
				dWindow = new PaintScreen();
				dataView = new DataView(mixContext);

				/*마지막으로 유저에게 선택된 값으로 데이터 뷰의 반경을 설정*/
				setZoomLevel();
				isInited = true;	// 세팅 플래그 true
			}

			/*애플리케이션이 처음 실행 되었을때 체크*/
			if(settings.getBoolean("firstAccess",false)==false){
				// 얼럿 다이얼로그를 생성하여
				AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
				// 라이센스 관련 메세지를 출력
				builder1.setMessage(getString(DataView.LICENSE_TEXT));
				// 닫기 버튼 처리
				builder1.setNegativeButton(getString(DataView.CLOSE_BUTTON), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});

				// 얼럿 다이얼로그 세팅 및 표시
				AlertDialog alert1 = builder1.create();
				alert1.setTitle(getString(DataView.LICENSE_TITLE));
				alert1.show();
				editor.putBoolean("firstAccess", true);
				editor.commit();	// 변경 사항이 완료되었으면 commit
			}

			// 정확한 위치를 찾지 못했을 경우(GPS 관련)
			if(mixContext.isActualLocation()==false){
				Toast.makeText( this, getString(DataView.CONNECTION_GPS_DIALOG_TEXT), Toast.LENGTH_LONG ).show();
			}

		} catch (Exception ex) {
			doError(ex);	// 예외 발생시 에러 처리
		}
	}

	// 인텐트 제어
	private void handleIntent(Intent intent) {
		// 검색 버튼을 눌렀을 경우
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);	// 쿼리 생성
			doMixSearch(query);	// 마커로부터 검색
		}
	}

	// 새 인텐트 생성시
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	// 검색 버튼을 눌렀을 경우 수행될 검색
	private void doMixSearch(String query) {
		DataHandler jLayer = dataView.getDataHandler();	// 데이터 핸들러 등록

		// 데이터 뷰가 얼어있지 않다면 리스트뷰와 맵으로부터 마커 리스트를 읽는다
		if(!dataView.isFrozen()){
			MixListView.originalMarkerList = jLayer.getMarkerList();
			MixMap.originalMarkerList = jLayer.getMarkerList();
		}


		// 검색 결과를 저장 할 리스트
		ArrayList<Marker> searchResults = new ArrayList<Marker>();


		// 검색된 항목이 1개 이상 있을 경우
		if (jLayer.getMarkerCount() > 0) {
			// 검색된 항목들을 결과 리스트에 추가
			for(int i = 0; i < jLayer.getMarkerCount(); i++) {
				Marker ma = jLayer.getMarker(i);
				if(ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1){
					searchResults.add(ma);
					/*타이틀에 상응하는 웹사이트들*/
				}
			}
		}
		// 결과 리스트에 하나라도 값이 있을 경우
		if (searchResults.size() > 0){
			dataView.setFrozen(true);	// 데이터 뷰를 얼리고
			jLayer.setMarkerList(searchResults);	// 결과를 핸들러에 할당
		}
		else	// 결과가 없을 경우엔 토스트 출력
			Toast.makeText( this, getString(DataView.SEARCH_FAILED_NOTIFICATION), Toast.LENGTH_LONG ).show();
	}

	// 중단 되었을 경우
	@Override
	protected void onPause() {
		super.onPause();

		try {
			this.mWakeLock.release();	// 웨이크 락을 풀어준다

			// 각 센서 관리자의 등록을 해제
			try {
				sensorMgr.unregisterListener(this, sensorGrav);
			} catch (Exception ignore) {
			}
			try {
				sensorMgr.unregisterListener(this, sensorMag);
			} catch (Exception ignore) {
			}
			try{
				sensorMgr_ori.unregisterListener(this,orientationSensor);
			}catch (Exception ignore){

			}
			sensorMgr = null;

			// 위치 관리자의 등록을 해제
			try {
				locationMgr.removeUpdates(this);
			} catch (Exception ignore) {
			}
			locationMgr = null;

			// 다운로드 관리자를 중지
			try {
				mixContext.downloadManager.stop();
			} catch (Exception ignore) {
			}

			// 에러 발생시에는 종료
			if (fError) {
				finish();
			}
		} catch (Exception ex) {
			doError(ex);	// 예외 발생시 에러 처리
		}
	}

	// 재개 되었을 경우. 대부분의 처리는 이곳에서...
	@Override
	protected void onResume() {
		super.onResume();



		try {
			this.mWakeLock.acquire();	// 웨이크 록

			killOnError();	// 에러 여부를 체크한다
			mixContext.mixView = this;	// 컨텍스트의 믹스뷰를 설정하고
			dataView.doStart();			// 데이터뷰 활성화
			dataView.clearEvents();		// 이벤트 클리어



			double angleX, angleY;		// 앵글의 x, y

			
			/*연산에 사용될 매트릭스 값들을 설정한다*/
			angleX = Math.toRadians(-90);
			m1.set(1f, 0f, 0f, 0f, (float) Math.cos(angleX), (float) -Math
					.sin(angleX), 0f, (float) Math.sin(angleX), (float) Math
					.cos(angleX));

			angleX = Math.toRadians(-90);
			angleY = Math.toRadians(-90);
			m2.set(1f, 0f, 0f, 0f, (float) Math.cos(angleX), (float) -Math
					.sin(angleX), 0f, (float) Math.sin(angleX), (float) Math
					.cos(angleX));
			m3.set((float) Math.cos(angleY), 0f, (float) Math.sin(angleY),
					0f, 1f, 0f, (float) -Math.sin(angleY), 0f, (float) Math
					.cos(angleY));

			m4.toIdentity();

			for (int i = 0; i < histR.length; i++) {
				histR[i] = new Matrix();
			}
			/*매트릭스 값 설정 완료*/

			// 센서 관리자 설정
			sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

			// 각 센서들 등록
			// 가속도 센서
			sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				sensorGrav = sensors.get(0);
			}

			// 지자기 센서
			sensors = sensorMgr.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
			if (sensors.size() > 0) {
				sensorMag = sensors.get(0);
			}
			//// TODO: 2016-06-01
			if(sensors.size()>0) {
				sensors = sensorMgr_ori.getSensorList(Sensor.TYPE_ORIENTATION);
				orientationSensor = sensors.get(0);
			}
			// 센서 관리자에 레지스터리스너를 이용해 각 센서들을 등록
			sensorMgr.registerListener(this, sensorGrav, SENSOR_DELAY_GAME);
			sensorMgr.registerListener(this, sensorMag, SENSOR_DELAY_GAME);

			if(orientationSensor!=null)
			{
				sensorMgr_ori.registerListener(this, orientationSensor,sensorMgr_ori.SENSOR_DELAY_GAME);
			}

			try {
				// 위치제공자의 기준(Criteria)
				// http://developer.android.com/reference/android/location/Criteria.html
				Criteria c = new Criteria();

				// 정확도 설정
				c.setAccuracy(Criteria.ACCURACY_FINE);
				//c.setBearingRequired(true);

				// 위치관리자를 할당 후
				locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				// 인자 값에 따라 위치 갱신을 요청한다. 2번째가 시간, 3번째가 거리  
				locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,4, this);

				// 최적의 위치제공자를 찾고, 할당 가능한지 파악한다
				String bestP = locationMgr.getBestProvider(c, true);
				isGpsEnabled = locationMgr.isProviderEnabled(bestP);

				// gps, 네트워크가 먹통일 때의 기본 위치 설정
				Location hardFix = new Location("reverseGeocoded");

				//				hardFix.setLatitude(0);
				//				hardFix.setLongitude(0);

				// 기본 위치의 값을 수정
				hardFix.setLatitude(46.480302);
				hardFix.setLongitude(11.296005);
				hardFix.setAltitude(300);

				/*New York*/
				//				hardFix.setLatitude(40.731510);
				//				hardFix.setLongitude(-73.991547);

				// TU Wien
//				hardFix.setLatitude(48.196349);
//				hardFix.setLongitude(16.368653);
//				hardFix.setAltitude(180);

				try {
					// 위치 관리자로부터 gps, 네트워크의 마지막으로 알려진 장소를 얻어 옮
					Location gps = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					Location network = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

					// 위치 값을 얻어오게 되면 현재 위치를 지정
					// 우선순위는 gps > 네트워크 > 기본값
					if(gps!=null)
						mixContext.curLoc = gps;
					else if (network!=null)
						mixContext.curLoc = network;
					else
						mixContext.curLoc = hardFix;

				} catch (Exception ex2) {	// 예외 발생 시
					ex2.printStackTrace();	// 메세지를 내보내고
					mixContext.curLoc = hardFix;	// 현재 위치는 기본값으로
				}
				// 최종적으로 지정된 값으로 컨텍스트의 위치를 세팅
				mixContext.setLocationAtLastDownload(mixContext.curLoc);

				// 지자기장의 설정. 밀리초 단위로 지정된 위치를 기준으로 삼는다
				GeomagneticField gmf = new GeomagneticField((float) mixContext.curLoc
						.getLatitude(), (float) mixContext.curLoc.getLongitude(),
						(float) mixContext.curLoc.getAltitude(), System
						.currentTimeMillis());

				// 지자기장의 값으로 매트릭스를 세팅
				angleY = Math.toRadians(-gmf.getDeclination());
				m4.set((float) Math.cos(angleY), 0f,
						(float) Math.sin(angleY), 0f, 1f, 0f, (float) -Math
						.sin(angleY), 0f, (float) Math.cos(angleY));
				mixContext.declination = gmf.getDeclination();
			} catch (Exception ex) {
				Log.d("mixare", "GPS Initialize Error", ex);	// 초기화 에러 로그
			}
			// 다운로드 스레드의 활성화
			downloadThread = new Thread(mixContext.downloadManager);
			downloadThread.start();

			//navi = new Navi2(mixContext);
			//tt//1 = new Thread(navi);
			//tt1.start();
			//navi = new NavigatorV2(mixContext);
			//tt1 = new Thread(navi);
			//tt1.start();

		} catch (Exception ex) {
			doError(ex);	// 에러 처리

			try {
				// 각각의 센서를 해제
				if (sensorMgr != null) {
					sensorMgr.unregisterListener(this, sensorGrav);
					sensorMgr.unregisterListener(this, sensorMag);
					sensorMgr = null;
				}
				// 위치 관리자도 해제 해 준다
				if (locationMgr != null) {
					locationMgr.removeUpdates(this);
					locationMgr = null;
				}
				// 컨텍스트의 다운로드 관리자도 정지
				if (mixContext != null) {
					if (mixContext.downloadManager != null)
						mixContext.downloadManager.stop();
				}
			} catch (Exception ignore) {
			}
		}



		// 알림 텍스트의 처리
		// 데이터 뷰가 얼지 않았을 때(기본 상태일 때) 내용을 표시하고 터치 가능하게 함 
		if (dataView.isFrozen() && searchNotificationTxt == null){
			searchNotificationTxt = new TextView(this);
			searchNotificationTxt.setWidth(dWindow.getWidth());
			searchNotificationTxt.setPadding(10, 2, 0, 0);
			searchNotificationTxt.setText(getString(DataView.SEARCH_ACTIVE_1)+" "+ mixContext.getDataSourcesStringList()+ getString(DataView.SEARCH_ACTIVE_2));;
			searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
			searchNotificationTxt.setTextColor(Color.WHITE);

			searchNotificationTxt.setOnTouchListener(this);
			addContentView(searchNotificationTxt, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
		else if(!dataView.isFrozen() && searchNotificationTxt != null){
			searchNotificationTxt.setVisibility(View.GONE);
			searchNotificationTxt = null;
		}
	}

	// 옵션 메뉴의 생성
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// 메뉴 버튼을 눌렀을 때 출력될 메뉴를 설정한다. 

		int base = Menu.FIRST;
		/*define the first*/
		MenuItem item1 =menu.add(base, base, base, getString(DataView.MENU_ITEM_1));
		MenuItem item2 =menu.add(base, base+1, base+1,  getString(DataView.MENU_ITEM_2));
		MenuItem item3 =menu.add(base, base+2, base+2,  getString(DataView.MENU_ITEM_3));
		MenuItem item4 =menu.add(base, base+3, base+3,  getString(DataView.MENU_ITEM_4));
		//MenuItem item5 =menu.add(base, base+4, base+4,  getString(DataView.MENU_ITEM_5));
		//MenuItem item6 =menu.add(base, base+5, base+5,  getString(DataView.MENU_ITEM_6));
		//MenuItem item7 =menu.add(base, base+6, base+6,  getString(DataView.MENU_ITEM_7));

		// 각 메뉴에 쓰일 아이콘 
		/*assign icons to the menu items*/
		item1.setIcon(drawable.icon_datasource);
		item2.setIcon(android.R.drawable.ic_menu_view);
		item3.setIcon(android.R.drawable.ic_menu_mapmode);
		item4.setIcon(android.R.drawable.ic_menu_zoom);
		//item5.setIcon(android.R.drawable.ic_menu_search);


		return true;
	}

	// 옵션 메뉴가 선택 되었을 경우의 처리
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		/*Data sources*/
			case 1:
				// 데이터 뷰가 작동시
				if (!dataView.isLauncherStarted()) {
					// 데이터 소스 리스트를 지정
					MixListView.setList(1);
					Intent intent = new Intent(MixView.this, MixListView.class);
					// 설정된 인텐트로 서브 액티비티를 만든다. 두번째 인자는 요청코드 번호
					startActivityForResult(intent, 40);
				} else {    // 아닐 시 토스트를 생성해 불가능을 알린다
					Toast.makeText(this, getString(DataView.OPTION_NOT_AVAILABLE_STRING_ID), Toast.LENGTH_LONG).show();
				}
				break;
		
		/*List view*/
			case 2:
				// 리스트 뷰의 리스트(각 소스의 내용들)를 지정
				MixListView.setList(2);
			/*대안의 리스트뷰에 표시할 타이틀 리스트가 비어있지 않을 경우*/
				if (dataView.getDataHandler().getMarkerCount() > 0) {
					Intent intent1 = new Intent(MixView.this, MixListView.class);
					// 설정된 인텐트로 서브 액티비티를 만든다
					startActivityForResult(intent1, 42);
				}
			/*리스트가 비어있을 경우*/
				else {
					Toast.makeText(this, DataView.EMPTY_LIST_STRING_ID, Toast.LENGTH_LONG).show();
				}
				break;
			
		/*Map View*/
			case 3:
                //TODO : mixmap말고 mixnavermap으로 map view 연결
                //Naver 지도 서브 액티비티
                Intent naverMapIntent = new Intent(this, MixNaverMap.class);
                List<Marker> markerList = dataView.getDataHandler().getMarkerList();
                JSONArray jsonArray = new JSONArray();
                try {
                    for(int i=0; i<markerList.size(); i++) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("title", markerList.get(i).getTitle());
                        jsonObject.put("longitude", markerList.get(i).getLongitude());
                        jsonObject.put("latitude", markerList.get(i).getLatitude());
                        jsonArray.put(i, jsonObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                naverMapIntent.putExtra("markerListJson", jsonArray.toString());
                startActivityForResult(naverMapIntent, 20);
                break;
		
		/*zoom level*/
			case 4:
				Location currentGPSInfo2 = mixContext.getCurrentGPSInfo();
				PhysicalPlace physicalPlace = new PhysicalPlace();
				physicalPlace.setLongitude(currentGPSInfo2.getLongitude());
				physicalPlace.setLatitude(currentGPSInfo2.getLatitude());
				Intent intent3 = new Intent(MixView.this,HttpPostSNS.class);
				intent3.putExtra("currentGPSInfo2",physicalPlace);
				startActivityForResult(intent3, 50);
				break;
			
		/*Search*/
			case 5:
				// 검색 처리를 한다
				onSearchRequested();
				break;
		
		/*GPS Information*/
			case 6:
				// 현재 GPS정보를 읽어오고
				Location currentGPSInfo = mixContext.getCurrentGPSInfo();
				// 얼럿 다이얼로그 빌더를 생성,
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				// 각 값들을 보여줄 메세지를 세팅한다
				builder.setMessage(getString(DataView.GENERAL_INFO_TEXT) + "\n\n" +
						getString(DataView.GPS_LONGITUDE) + currentGPSInfo.getLongitude() + "\n" +
						getString(DataView.GPS_LATITUDE) + currentGPSInfo.getLatitude() + "\n" +
						getString(DataView.GPS_ALTITUDE) + currentGPSInfo.getAltitude() + "m\n" +
						getString(DataView.GPS_SPEED) + currentGPSInfo.getSpeed() + "km/h\n" +
						getString(DataView.GPS_ACCURACY) + currentGPSInfo.getAccuracy() + "m\n" +
						getString(DataView.GPS_LAST_FIX) + new Date(currentGPSInfo.getTime()).toString() + "\n");
				// 닫기 버튼의 처리
				builder.setNegativeButton(getString(DataView.CLOSE_BUTTON), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});


		}
		return true;
	}
	
	// 줌 레벨을 계산
	public float calcZoomLevel(){
		// 줌 바로 부터 현제 프로그레스 값(줌 레벨)을 읽는다  
		int myZoomLevel = myZoomBar.getProgress();
		float myout = 5;

		// 수치에 따른 줌 레벨 계산. 적절한 값을 할당하기 위해 추가 계산을 한다
		if (myZoomLevel <= 26) {
			myout = myZoomLevel / 25f;
		} else if (25 < myZoomLevel && myZoomLevel < 50) {
			myout = (1 + (myZoomLevel - 25)) * 0.38f;
		} 
		else if (25== myZoomLevel) {
			myout = 1;
		} 
		else if (50== myZoomLevel) {
			myout = 10;
		} 
		else if (50 < myZoomLevel && myZoomLevel < 75) {
			myout = (10 + (myZoomLevel - 50)) * 0.83f;
		} else {
			myout = (30 + (myZoomLevel - 75) * 2f);
		}

		return myout;	// 계산된 값 리턴
	}

	// 줌 레벨 지정
	private void setZoomLevel() {
		float myout = calcZoomLevel();	// 줌 레벨은 우선 특정한 방식으로 계산되어야 한다

		dataView.setRadius(myout);	// 계산된 값으로 데이터 뷰의 반경을 설정 후

		// 줌 바를 감추면서 줌 레벨을 지정한다
		myZoomBar.setVisibility(View.INVISIBLE);
		zoomLevel = String.valueOf(myout);

		// 재설정된 반경을 적용하기 위한 데이터뷰 재시작
		dataView.doStart();
		dataView.clearEvents();	// 이벤트를 클리어 하고
		// 내용을 다시 다운로드 한다
		downloadThread = new Thread(mixContext.downloadManager);
		downloadThread.start();

	};
	
	// 시크바 체인지 리스너. 줌 레벨 설정시의 줌 바를 처리하기 위함
	private SeekBar.OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		Toast t;	// 결과를 알릴 토스트

		// 프로그레스가 변경될 경우
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			float myout = calcZoomLevel();	// 줌 레벨을 계산

			// 각 값들을 변경하고
			zoomLevel = String.valueOf(myout);
			zoomProgress = myZoomBar.getProgress();

			// 변경 사항을 출력해 준다
			t.setText("Radius: " + String.valueOf(myout));
			t.show();
		}

		// 프로그레스를 터치하여 트랙킹 중일 경우
		public void onStartTrackingTouch(SeekBar seekBar) {
			Context ctx = seekBar.getContext();
			// 토스트의 텍스트를 변경
			t = Toast.makeText(ctx, "Radius: ", Toast.LENGTH_LONG);
//			zoomChanging= true;
		}

		// 트랙킹이 멈췄을 경우
		public void onStopTrackingTouch(SeekBar seekBar) {
			// 공유 프레퍼런스를 읽어오고 
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();			
			/*유저로부터 선택된 줌 반경을 등록한다*/
			editor.putInt("zoomLevel", myZoomBar.getProgress());
			editor.commit();
			
			// 줌 바는 감춘다
			myZoomBar.setVisibility(View.INVISIBLE);
//			zoomChanging= false;

			myZoomBar.getProgress();

			t.cancel();
			setZoomLevel();	// 값의 지정이 끝났으면 줌 레벨을 설정
		}

	};

	// 센서값이 변경될 경우 처리
	public void onSensorChanged(SensorEvent evt) {
		try {
			//			killOnError();

			// 가속도 센서, 지자기 센서 각각의 경우 지정된 변수에 값을 넣는다
			if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				grav[0] = evt.values[0];
				grav[1] = evt.values[1];
				grav[2] = evt.values[2];

				augScreen.postInvalidate();	// 변경을 알림
			} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				mag[0] = evt.values[0];
				mag[1] = evt.values[1];
				mag[2] = evt.values[2];

				augScreen.postInvalidate();	// 변경을 알림
			}

			// 회전 행렬값들을 저장
			SensorManager.getRotationMatrix(RTmp, I, grav, mag);
			// 축 변경(?)
			SensorManager.remapCoordinateSystem(RTmp, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, Rot);

			// 임시 행렬에 값들을 지정
			tempR.set(Rot[0], Rot[1], Rot[2], Rot[3], Rot[4], Rot[5], Rot[6], Rot[7],
					Rot[8]);

			// 단위 행렬로 세팅하고 행렬 곱 연산 후, 역행렬로 만든다
			finalR.toIdentity();
			finalR.prod(m4);
			finalR.prod(m1);
			finalR.prod(tempR);
			finalR.prod(m3);
			finalR.prod(m2);
			finalR.invert(); 

			// 이후 부분에 대해서는 더 분석이 필요할 것 같다...
			histR[rHistIdx].set(finalR);
			rHistIdx++;
			if (rHistIdx >= histR.length)
				rHistIdx = 0;

			smoothR.set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
			for (int i = 0; i < histR.length; i++) {
				smoothR.add(histR[i]);
			}
			smoothR.mult(1 / (float) histR.length);

			synchronized (mixContext.rotationM) {
				mixContext.rotationM.set(smoothR);
			}
			synchronized (this){
                if(navi.isStart)
    				navi.setOrientation(evt.values[0]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// 터치 이벤트 처리
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		try {
			killOnError();	// 에러 여부를 판단

			// 눌려진 부분의 x, y 좌표를 저장한다
			float xPress = me.getX();
			float yPress = me.getY();
			
			// 손을 뗏을 경우 데이터 뷰의 클릭이벤트 처리
			if (me.getAction() == MotionEvent.ACTION_UP) {
				dataView.clickEvent(xPress, yPress);
			}

			return true;
			
		} catch (Exception ex) {	// 예외 처리
			//doError(ex);
			ex.printStackTrace();
			return super.onTouchEvent(me);
		}
	}

	// 키 다운에 관한 처리. 실제 단말기에선 필요가 없을듯?
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			killOnError();

			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (dataView.isDetailsView()) {
					dataView.keyEvent(keyCode);
					dataView.setDetailsView(false);
					return true;
				} else {
					return super.onKeyDown(keyCode, event);
				}
			} else if (keyCode == KeyEvent.KEYCODE_MENU) {
				return super.onKeyDown(keyCode, event);
			}
			else {
				dataView.keyEvent(keyCode);
				return false;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return super.onKeyDown(keyCode, event);
		}
	}

	// 위치제공자가 불능의 경우
	public void onProviderDisabled(String provider) {
		isGpsEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	// 위치제공자가 사용 가능한 경우. 굳이 따로 나눌 필요가 있었을까?
	public void onProviderEnabled(String provider) {
		isGpsEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	// 상태 변화시 처리. 일단은 비워둔다
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	// 위치가 변경되었을 경우
	public void onLocationChanged(Location location) {

		try {
			killOnError();	// 에러 여부 체크
			gpsLat=location.getLatitude();
			gpsLong=location.getLongitude();
			navi.setGPSLatLng(gpsLat, gpsLong);
			// 변경된 위치의 로그를 생성한다. 위도와 경도를 기록
			Log.v(TAG,"Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
			
			// 위치 제공자가 동일할 경우
			if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
				// 현재의 위치를 변경된 위치로 지정
				synchronized (mixContext.curLoc) {
					mixContext.curLoc = location;
				}
				// 데이터 뷰가 얼어있지 않다면
				if(!dataView.isFrozen())	// 데이터뷰의 데이터 핸들러에 위치변경 통보
					dataView.getDataHandler().onLocationChanged(location);
				
				/* 만약 마지막으로 데이터를 다운로드한 곳으로부터 반경 3km 이상 *
				 * 떨어진 곳으로 이동한다면, 데이터를 새로 다운로드 해야 한다	 */
				// 마지막으로 다운로드된 위치를 저장 
				Location lastLoc = mixContext.getLocationAtLastDownload();
				if(lastLoc==null)	// 기록된 위치가 없다면
					mixContext.setLocationAtLastDownload(location);	// 변경된 위치를 마지막 위치로 지정
				else {
					// 경계 설정
					float threshold = dataView.getRadius()*1000f/3f;
					Log.v(TAG,"Location Change: "+" threshold "+threshold+" distanceto "+location.distanceTo(lastLoc));
					// 설정 경계를 넘어가면 데이터를 다시 다운로드한다
					if(location.distanceTo(lastLoc)>threshold)  {
						Log.d(TAG,"Restarting download due to location change");
						dataView.doStart();
					}	
				}
				isGpsEnabled = true;	// GPS는 가능한 상태로
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// 정확도가 변경되었을 경우
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// 자기장 센서의 값이 명확치 않을 경우
		if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE && compassErrorDisplayed == 0) {
			for(int i = 0; i <2; i++) {
				// 나침반 데이터가 부정확합니다. 나침반을 수정하십시오
				Toast.makeText(mixContext, "Compass data unreliable. Please recalibrate compass.", Toast.LENGTH_LONG).show();
			}
			compassErrorDisplayed++;
		}
	}

	// 터치시 발생. onTouchEvent 보다 먼저 발생한다 
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		dataView.setFrozen(false);	// 데이터뷰 해동
		if (searchNotificationTxt != null) {	// 알림텍스트가 있다면
			searchNotificationTxt.setVisibility(View.GONE);	// 사라지게 함
			searchNotificationTxt = null;
		}
		return false;
	}
}

/**
 * @author daniele
 *
 */
/**
 * @author daniele
 *
 */

// 카메라 뷰(카메라 서페이스) 클래스
class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
	MixView app;	// 메인 뷰
	SurfaceHolder holder;	// 서페이스 홀더
	Camera camera;

	// 생성자
	CameraSurface(Context context) {
		super(context);

		try {
			app = (MixView) context;	// 컨텍스트(메인 뷰)를 등록

			// 홀더를 읽어오고 콜백을 등록한다
			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);	// 푸쉬버퍼 사용
		} catch (Exception ex) {

		}
	}

	// 서페이스 생성시
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			// 카메라가 열려있다면 정지하고 해제
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
				}
				try {
					camera.release();
				} catch (Exception ignore) {
				}
				camera = null;
			}

			camera = Camera.open();	// 카메라 오픈
			camera.setPreviewDisplay(holder);	// 서페이스 홀더에 영상을 출력할 곳 지정
		} catch (Exception ex) {
			try {	// 예외 발생시 카메라 정지 및 해제
				if (camera != null) {
					try {
						camera.stopPreview();
					} catch (Exception ignore) {
					}
					try {
						camera.release();
					} catch (Exception ignore) {
					}
					camera = null;
				}
			} catch (Exception ignore) {

			}
		}
	}

	// 서페이스 파괴시
	public void surfaceDestroyed(SurfaceHolder holder) {
		try {	// 카메라 정지 및 해제
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
				}
				try {
					camera.release();
				} catch (Exception ignore) {
				}
				camera = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// 서페이스 변경시
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			// 일단 카메라의 파라메터 값들을 읽어온다
			Camera.Parameters parameters = camera.getParameters();
			try {
				// 지원되는 카메라의 사이즈 리스트
				List<Camera.Size> supportedSizes = null;
				
				// 이하의 코드는 안드로이드 1.6버전 미만에서는 작동하지 않는다
				// 그럼에도 불구하고 카메라는 돌아가겠지만...
				
				// 파라메터로부터 지원되는 프리뷰 사이즈들을 읽어온다
				supportedSizes = Compatibility.getSupportedPreviewSizes(parameters);

				// 프리뷰 형태의 팩터
				float ff = (float)w/h;
				Log.d("Mixare", "Screen res: w:"+ w + " h:" + h + " aspect ratio:" + ff);

				//holder for the best form factor and size 
				// 최적의 형태의 팩터와 사이즈의 홀더를 구한다
				float bff = 0;
				int bestw = 0;
				int besth = 0;
				Iterator<Camera.Size> itr = supportedSizes.iterator();
				
				// 우리는 최적의 프리뷰 사이즈를 찾을 것이고
				// 이것은 스크린 형태의 팩터에 가까워야 하며, 스크린 자체보다는 넓지 않아야 한다
				// 차후의 요구사항은 2.1버전의 HTC Hero 의 경우엔 스크린보다 큰 카메라 프리뷰 사이즈를
				// 리포트할 것 이고, 이는 카메라의 초기화를 실패하게 할 것이기 때문이다.
				// 하지만 다른 기기에서는 스크린보다 큰 프리뷰도 제대로 작동할 것이다
				
				// 반복자(이터레이터)를 이용해 최적의 사이즈를 찾는다
				while(itr.hasNext()) {
					Camera.Size element = itr.next();

					// 현재 형태의 팩터 (current form factor) 
					float cff = (float)element.width/element.height;
					
					// 각 요소들의 변수값을 로그로 기록
					Log.d("Mixare", "Candidate camera element: w:"+ element.width + " h:" + element.height + " aspect ratio:" + cff);
					
					// 현재 요소가 여태까지의 최적의 결과를 대체할 수 있는지 체크
					// 현재 형태의 요소는 최적의 팩터에 가까울 것이고
					// 프리뷰의 넓이는 스크린 넓이보다 작고, 최적의 넓이보다는 넓어야 한다
					// * 이 조합은 보다 나은 해법을 보장하게 될 것이다
					if ((ff-cff <= ff-bff) && (element.width <= w) && (element.width >= bestw)) {
						bff=cff;
						bestw = element.width;
						besth = element.height;
					}
				} 
				// 최종적인 결과 수치를 로그에 기록한다
				Log.d("Mixare", "Chosen camera element: w:"+ bestw + " h:" + besth + " aspect ratio:" + bff);

				// 몇몇 삼성 제품에서는 최적의 넓이와 높이가 0으로 판명날 것이다.
				// 이는 그 제품들에서는 최소의 프리뷰 사이즈가 스크린 사이즈보다 크기 때문인데,
				// 이런 경우에는 디폴트 값(480*320)을 주도록 한다
				if ((bestw == 0) || (besth == 0)){
					Log.d("Mixare", "Using default camera parameters!");
					bestw = 480;
					besth = 320;
				}
				parameters.setPreviewSize(bestw, besth);	// 프리뷰 사이즈 최종 설정
			} catch (Exception ex) {	// 예외 발생시에도 디폴트 값으로...
				parameters.setPreviewSize(480 , 320);
			}

			// 모든 값이 입력된 파라메터를 카메라에 적용하고
			camera.setParameters(parameters);
			camera.startPreview();	// 프리뷰를 시작한다
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

// 증강된 뷰
class AugmentedView extends View {
	MixView app;	// 메인 뷰
	
	// 클래스 외부에서 사용될 변수들(?)
	int xSearch = 200;
	int ySearch = 10;
	int searchObjWidth = 0;
	int searchObjHeight = 0;

	// 생성자
	public AugmentedView(Context context) {
		super(context);

		try {
			app = (MixView) context;	// 컨텍스트(메인 뷰)를 등록

			app.killOnError();	// 에러 여부를 체크
		} catch (Exception ex) {
			app.doError(ex);
		}
	}

	// 그려지는 부분
	@Override
	protected void onDraw(Canvas canvas) {
		try {
			//			if (app.fError) {
			//
			//				Paint errPaint = new Paint();
			//				errPaint.setColor(Color.RED);
			//				errPaint.setTextSize(16);
			//				
			//				/*Draws the Error code*/
			//				canvas.drawText("ERROR: ", 10, 20, errPaint);
			//				canvas.drawText("" + app.fErrorTxt, 10, 40, errPaint);
			//
			//				return;
			//			}

			app.killOnError();	// 에러 여부를 체크

			// 캔버스의 넓이만큼 뷰로 설정(뷰의 넓이만큼을 캔버스로 이용한다)
			MixView.dWindow.setWidth(canvas.getWidth());
			MixView.dWindow.setHeight(canvas.getHeight());

			MixView.dWindow.setCanvas(canvas);	// 캔버스와 연결

			// 데이터 뷰가 초기화 되지 않았을 경우엔 초기화 처리
			if (!MixView.dataView.isInited()) {
				MixView.dataView.init(MixView.dWindow.getWidth(), MixView.dWindow.getHeight());
			}
			
			// 줌 바가 보여지고 있을 경우
			if (app.isZoombarVisible()){
				
				// 줌에 관련된 정보를 표시할 페인트 객체 설정
				Paint zoomPaint = new Paint();
				zoomPaint.setColor(Color.WHITE);
				zoomPaint.setTextSize(14);
				
				// 거리를 나타낼 스트링 값
				String startKM, endKM;
				endKM = "80km";
				startKM = "0km";
				/*if(MixListView.getDataSource().equals("Twitter")){
					startKM = "1km";
				}*/
				
				// 거리 정보 텍스트 출력
				canvas.drawText(startKM, canvas.getWidth()/100*4, canvas.getHeight()/100*85, zoomPaint);
				canvas.drawText(endKM, canvas.getWidth()/100*99+25, canvas.getHeight()/100*85, zoomPaint);

				// 줌 레벨을 출력
				int height= canvas.getHeight()/100*85;
				int zoomProgress = app.getZoomProgress();
				if (zoomProgress > 92 || zoomProgress < 6) {
					height = canvas.getHeight()/100*80;
				}
				canvas.drawText(app.getZoomLevel(),  (canvas.getWidth())/100*zoomProgress+20, height, zoomPaint);
			}

			// 데이터 뷰의 데이터들을 윈도우에 그린다
			MixView.dataView.draw(MixView.dWindow);
		} catch (Exception ex) {
			app.doError(ex);
		}
	}
}
