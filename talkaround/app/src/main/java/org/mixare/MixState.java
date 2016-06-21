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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.DataSource;
import org.mixare.data.Json;
import org.mixare.reality.PhysicalPlace;
import org.mixare.render.Matrix;
import org.mixare.render.MixVector;

import java.util.List;

// 현재의 상태에 관한 클래스
public class MixState {

	// 각 상태에 대한 상수값 설정
	public static int NOT_STARTED = 0;
	public static int PROCESSING = 1;
	public static int READY = 2;
	public static int DONE = 3;

	int nextLStatus = MixState.NOT_STARTED;	// 다음 상태
	String downloadId;	// 다운로드할 ID

	private float curBearing;	// 현재의 방위각
	private float curPitch;		// 현재의 장치각(?)

	private boolean detailsView;	// 디테일 뷰가 표시 중인지 여부

	// 이벤트 처리
	public boolean handleEvent(MixContext ctx, String onPress, String title, PhysicalPlace log) {
		/*CharSequence cs1 = "-";
		// 눌려진 스트링 값이 null 이 아니고, 웹페이지로 연결될 경우
		if (onPress != null && onPress.startsWith("webpage")) {
			try {
				// 내용을 파싱하고 디테일 뷰에 웹페이지를 띄운다
				String webpage = MixUtils.parseAction(onPress);
				this.detailsView = true;
				ctx.loadMixViewWebPage(webpage);
			} catch (Exception ex) {
			}
		}
		else{*/
		DialogSelectOption(ctx, title, log, onPress);
		//}


		//else if(onPress != null && onPress.contains(cs1)) { // 버스정보에요
		//
		//	//new HttpAsyncTask().execute("http://hmkcode.appspot.com/rest/controller/get.json");

		//	//DownloadRequest requestData = new DownloadRequest();

		//	//requestData(DataSource.createRequestURL(source, lat, lon, alt, radius, Locale.getDefault().getLanguage()), DataSource.dataFormatFromDataSource(source), source);


		//}
		return true;
	}

	public void DialogSelectOption(final MixContext ctx, final String markerTitle, final PhysicalPlace log, final String onPress) {
		final String items[] = {"자세히 보기", "지도에서 길 찾기", "네비게이션" };
		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setTitle(markerTitle);
		ab.setItems(items,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// 프로그램을 종료한다
						Toast.makeText(ctx,
								items[id] + " 선택했습니다.",
								Toast.LENGTH_SHORT).show();
						dialog.dismiss();

						if (id == 1) {
							//TODO : 길찾기 마커 리스트 넘기기
							Intent markerNaverIntent = new Intent(ctx, MixNaverMap.class);
							markerNaverIntent.putExtra("latitude", log.getLatitude());
							markerNaverIntent.putExtra("longitude", log.getLongitude());
							markerNaverIntent.putExtra("marker_title", markerTitle);
							ctx.startActivity(markerNaverIntent);

						} else if (id == 2) {

							if(ctx.getCurrentLocation().getLongitude() != 0 && ctx.getCurrentLocation().getLatitude() !=0) {

								final String url = DataSource.createNaverMapRequestURL(ctx.getCurrentLocation().getLongitude(),ctx.getCurrentLocation().getLatitude(), log.getLongitude(), log.getLatitude());

								String result = "";
								//HttpConnection httpConnection = new HttpConnection();
								try {
									result = (new HttpConnection()).execute(url).get();

									Log.d("NaverJson", result);
									Toast.makeText(ctx,result,Toast.LENGTH_LONG).show();

									List<Marker> pathMarkerList = parseJSONtoMarker(result);

									Log.d("NaverJson2", pathMarkerList.get(0).getTitle());
									Toast.makeText(ctx,pathMarkerList.get(0).getTitle(),Toast.LENGTH_LONG).show();

									Navi2.pathMarkerList = pathMarkerList;
									Log.d("NaverGetPath", "get Json data done");

								} catch (Exception e) {
									Log.e("HttpConnection", "" + e);
								}

								Navi2.isStart = true;
							}
						//else
						//{
						//	//Toast.makeText(ctx, "gps 없음", Toast.LENGTH_SHORT).show();
						//}
							//GPSThread.isStart=true;
						} else if (id == 0) {
							try {
								String webpage = MixUtils.parseAction(onPress);
								//this.detailsView = true;
								ctx.loadMixViewWebPage(webpage);
							} catch (Exception e) {
							}
						}
					}
				}
		);
		// 다이얼로그 생성
		AlertDialog alertDialog = ab.create();
		// 다이얼로그 보여주기
		alertDialog.show();
	}

	private List<Marker> parseJSONtoMarker(String json) throws JSONException {
		JSONObject root = new JSONObject(json);
		Json jsonClass = new Json();
		List<Marker> markers = jsonClass.load(root, DataSource.DATAFORMAT.NAVER);

		return markers;
	}

	// 현재의 방위각을 리턴
	public float getCurBearing() {
		return curBearing;
	}

	// 현재의 장치각을 리턴
	public float getCurPitch() {
		return curPitch;
	}

	// 디테일 뷰의 표시 여부를 리턴
	public boolean isDetailsView() {
		return detailsView;
	}

	// 디테일 뷰의 표시 여부를 설정
	public void setDetailsView(boolean detailsView) {
		this.detailsView = detailsView;
	}

	// 장치각과 방위각을 계산
	public void calcPitchBearing(Matrix rotationM) {
		MixVector looking = new MixVector();
		rotationM.transpose();
		looking.set(1, 0, 0);
		looking.prod(rotationM);
		this.curBearing = (int) (MixUtils.getAngle(0, 0, looking.x, looking.z)  + 360 ) % 360 ;

		rotationM.transpose();
		looking.set(0, 1, 0);
		looking.prod(rotationM);
		this.curPitch = -MixUtils.getAngle(0, 0, looking.y, looking.z);
	}
}
