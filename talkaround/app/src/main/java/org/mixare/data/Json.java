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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.Marker;
import org.mixare.MixView;
import org.mixare.NaverMarker;
import org.mixare.SnsMarker;
import org.mixare.SocialMarker;
import org.mixare.data.DataSource.DATAFORMAT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// JSON 파일을 다루는 클래스
public class Json extends DataHandler {

    public static final int MAX_JSON_OBJECTS = 100;    // JSON 객체의 최대 수

    // 각종 데이터를 로드
    public List<Marker> load(JSONObject root, DATAFORMAT dataformat) {
        // 데이터를 읽는데 사용할 JSON 객체와 데이터행렬, 마커들
        JSONObject jo = null;
        JSONArray dataArray = null;
        List<Marker> markers = new ArrayList<Marker>();

        try {
            if(dataformat == DATAFORMAT.BUSSTOP)
                dataArray = root.getJSONObject("result").getJSONArray("station");
            else if(dataformat == DATAFORMAT.SNS || dataformat == DATAFORMAT.SCHOOL || dataformat == DATAFORMAT.SCHOOLRestaurant)
                dataArray = root.getJSONArray("list");
                //네이버 주변정보
            else if (root.has("result") && root.getJSONObject("result").has("site"))
                dataArray = root.getJSONObject("result").getJSONArray("site");
                //네이버 길찾기
            else
                dataArray = root.getJSONObject("result").getJSONArray("route").getJSONObject(0).getJSONArray("point");

            Log.i("dataArray값", dataArray.toString());
            // 데이터행렬에 데이터들이 있다면
            if (dataArray != null) {
                // 일단 로그 생성. 데이터 포맷을 기록한다
                Log.i(MixView.TAG, "processing " + dataformat + " JSON Data Array");
                // 최대 객체 수와 실제 데이터 길이를 비교해 최소치를 탑으로 지정
                int top = Math.min(MAX_JSON_OBJECTS, dataArray.length());

                // 각 데이터들에 대한 처리
                for (int i = 0; i < top; i++) {
                    // 처리할 JSON 객체를 할당
                    jo = dataArray.getJSONObject(i);
                    Log.i("JSON값", jo.toString());

                    Marker ma = null;
                    // 데이터 포맷에 따른 처리
                    switch (dataformat) {
                        case SCHOOL:
                            ma = processSCHOOLJSONObject(jo);
                            break;
                        case SCHOOLRestaurant:
                            ma = processSCHOOLRestaurantJSONObject(jo);
                            break;
                        case CAFE:
                            ma = processCAFEJSONObject(jo);
                            break;
                        case BUSSTOP:
                            ma = processBusJSONObject(jo);
                            break; // 특수처리
                        case Convenience:
                            ma = processConvenienceJSONObject(jo);
                            break;
                        case Restaurant:
                            ma = processRestaurantJSONObject(jo);
                            break;
                        case SNS:
                            ma = processSNSJSONObject(jo);
                            break;
                        case NAVER:
                            ma = processNaverJSONObject(jo);
                            break;

                        default:
                            ma = processMixareJSONObject(jo);
                            break;
                    }
                    // 마커 추가
                    if (ma != null)
                        markers.add(ma);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 모든 마커가 추가된 리스트를 리턴
        return markers;
    }

    //TODO : process naver json
    // 자체 데이터의 처리
    public Marker processNaverJSONObject(JSONObject jo) throws JSONException {

        Marker ma = null;	// 임시객체

        if(jo.has("key")) {
            ma = new NaverMarker(
                    jo.getString("key"),
                    /*jo.getDouble("y"),
                    jo.getDouble("x"),*/
                    jo.getInt("y"),
                    jo.getInt("x"),
                    0,
                    null,
                    DataSource.DATASOURCE.NAVER,
                    jo.getJSONObject("road").getDouble("distance"),
                    jo.getJSONObject("guide").getString("name")
            );

        }

        return ma;	// 마커 리턴
    }

    public Marker processConvenienceJSONObject(JSONObject jo)  throws JSONException {
        Marker ma = null;

        // 형식에 맞는지 검사. 타이틀과 위도, 경도, 고도 태그를 찾는다
        if (jo.has("x") && jo.has("y") && jo.has("name")) {
            Log.v(MixView.TAG, "processing Mixare JSON object");    // 로그 출력
            String link = "http://www.naver.com";

            // TODO: 2016-05-31 link 에 관한 처리를 합시다.
            // 웹페이지의 형식을 검사하고 스트링 값을 읽어온다
            if (jo.has("has_detail_page") && jo.getInt("has_detail_page") != 0 && jo.has("webpage"))
                link = jo.getString("webpage");

            // 할당된 값들로 마커 생성, // 일단은 경도, 위도, 이름만.
            // 맨뒤에값은 플래그 일단 Flag 0 는 카페정보
            ma = new SocialMarker(
                    jo.getString("name"),
                    jo.getDouble("y"),
                    jo.getDouble("x"),
                    0,
                    null,
                    DataSource.DATASOURCE.Convenience,"CONVENICE");
        }
        return ma;    // 마커 리턴
    }

    public Marker processRestaurantJSONObject(JSONObject jo) throws JSONException {
        Marker ma = null;

        // 형식에 맞는지 검사. 타이틀과 위도, 경도, 고도 태그를 찾는다
        if (jo.has("x") && jo.has("y") && jo.has("name")) {

            // 할당된 값들로 마커 생성, // 일단은 경도, 위도, 이름만.
            // 맨뒤에값은 플래그 일단 Flag 0 는 카페정보
            ma = new SocialMarker(
                    jo.getString("name"),
                    jo.getDouble("y"),
                    jo.getDouble("x"),
                    0,
                    null,
                    DataSource.DATASOURCE.Restaurant, "RESTRAUNT");
        }
        return ma;    // 마커 리턴
    }

    public Marker processCAFEJSONObject(JSONObject jo)  throws JSONException {
        Marker ma = null;

        // 형식에 맞는지 검사. 타이틀과 위도, 경도, 고도 태그를 찾는다
        if (jo.has("x") && jo.has("y") && jo.has("name")) {

            // 할당된 값들로 마커 생성, // 일단은 경도, 위도, 이름만.
            // 맨뒤에값은 플래그 일단 Flag 0 는 카페정보
            ma = new SocialMarker(
                    jo.getString("name"),
                    jo.getDouble("y"),
                    jo.getDouble("x"),
                    0,
                    null,
                    DataSource.DATASOURCE.CAFE, "CAFE");
        }
        return ma;    // 마커 리턴
    }

    public Marker processSCHOOLRestaurantJSONObject(JSONObject jo)  throws JSONException{
        Marker ma = null;
        int tempNum;
        if(jo.has("name")) {
            //tempNum = jo.getInt("id");
            //String tempLink = "http://khu.menu/global/" + Integer.toString(tempNum);
            ma = new SocialMarker(jo.getString("name"),jo.getDouble("latitude"),jo.getDouble("longitude"),0,jo.getString("url"), DataSource.DATASOURCE.SCHOOLRestaurant,jo.getString("icon"));
        }
        return ma;
    }

    public Marker processSCHOOLJSONObject(JSONObject jo)  throws JSONException {

        Marker ma = null;
        String tempStr = null;
        String linkUrl = null;
        if(jo.has("name")) {

            ma = new SocialMarker(jo.getString("name"),jo.getDouble("latitude"),jo.getDouble("longitude"),0,jo.getString("url"), DataSource.DATASOURCE.SCHOOL,jo.getString("icon"));
        }

        return ma;
    }

    //// 버즈 데이터의 처리
    //public Marker processBuzzJSONObject(JSONObject jo) throws NumberFormatException, JSONException {
    //	Marker ma = null;	// 임시객체
    //
    //	// 버즈 데이터의 형식에 맞는지 검사한다. title, geocode, links 태그가 있어야 한다.
    //	if (jo.has("title") && jo.has("geocode") && jo.has("links")) {
    //		Log.v(MixView.TAG, "processing Google Buzz JSON object");	// 로그 출력
//
    //		// 확인된 태그에 따라 값을 추출하여 소셜 마커를 생성한다
    //		ma = new SocialMarker(
    //				jo.getString("title"),
    //				Double.valueOf(jo.getString("geocode").split(" ")[0]),
    //				Double.valueOf(jo.getString("geocode").split(" ")[1]),
    //				0,
    //				jo.getJSONObject("links").getJSONArray("alternate").getJSONObject(0).getString("href"),
    //				DataSource.DATASOURCE.BUZZ);
    //	}
    //	return ma;	// 마커 리턴
    //}

    //// 트위터 데이터의 처리
    //public Marker processTwitterJSONObject(JSONObject jo) throws NumberFormatException, JSONException {
    //	Marker ma = null;	// 임시객체
    //
    //	// 일단 트위터 형식에 맞는지 검사한다
    //	if (jo.has("geo")) {
    //		Double lat=null, lon=null;	// 위도, 경도 값
    //
    //		// 지역 정보일 경우 각 정보를 할당
    //		if(!jo.isNull("geo")) {
    //			JSONObject geo = jo.getJSONObject("geo");
    //			JSONArray coordinates = geo.getJSONArray("coordinates");	// 좌표?
    //			lat=Double.parseDouble(coordinates.getString(0));
    //			lon=Double.parseDouble(coordinates.getString(1));
    //		}
    //		else if(jo.has("location")) {	// 위치 정보일 경우
//
    //			// Regex pattern to match location information
    //			// from the location setting, like:
    //			// iPhone: 12.34,56.78
    //			// ÜT: 12.34,56.78
    //			// 12.34,56.78
    //
    //			// 패턴매칭? 맙소사.. 아직 모르겠다
    //			Pattern pattern = Pattern.compile("\\D*([0-9.]+),\\s?([0-9.]+)");
    //			Matcher matcher = pattern.matcher(jo.getString("location"));
//
    //			// 어쨋든 발견했을 경우 위도와 경도를 입력
    //			if(matcher.find()){
    //				lat=Double.parseDouble(matcher.group(1));
    //				lon=Double.parseDouble(matcher.group(2));
    //			}
    //		}
    //		// 위치를 찾았을 경우
    //		if(lat!=null) {
    //			Log.v(MixView.TAG, "processing Twitter JSON object");	// 로그 출력
    //			// 유저와 정보 url 을 지정하기 위한 스트링
    //			String user=jo.getString("from_user");
    //			String url="http://twitter.com/"+user;
    //
    //			// 지정된 값에 따라 마커를 생성
    //			ma = new SocialMarker(
    //					user+": "+jo.getString("text"),
    //					lat,
    //					lon,
    //					0, url,
    //					DataSource.DATASOURCE.TWITTER);
    //		}
    //	}
    //	return ma;	// 마커 리턴
    //}

    public Marker processArriveBusJSONObject(JSONObject jo) throws JSONException {
        Marker ma = null;


      //  if(jo.getJSONObject("result").getInt("resultCode") != 0) {
//
      //      ma = new SocialMarker(
      //              "좀 되라 시발 좀 되라고",
      //              0,
      //              0,
      //              0,
      //              "",
      //              DataSource.DATASOURCE.ARRIVEBUS, 10);
      //  }
        return ma;

    }

    public Marker processBusJSONObject(JSONObject jo) throws JSONException {

        Marker ma = null;

        if (jo.has("x") && jo.has("y") && jo.has("stationDisplayID") && jo.has("stationDisplayName")) {// 버스정류장 정보일 경우

            String link = jo.getString("stationDisplayID");
            String[] sTemp = link.split("-");
            link = "";
            String tempLink;

            for(int i =0 ; i<sTemp.length; i++) {
                link += sTemp[i];
            }
            tempLink = ("http://lab.khlug.org/manapie/bus_arrival.php?station=" + link);

            ma = new SocialMarker(
                    jo.getString("stationDisplayName"),
                    jo.getDouble("y"),
                    jo.getDouble("x"),
                    0,
                    tempLink,
                    DataSource.DATASOURCE.BUSSTOP,"BUSSTOP");
            // TODO: 2016-05-31 이 부분은 나중에 웹페이지에 연결을 하던지 하자

        }
        return ma;
    }

    // 자체 데이터의 처리
    public Marker processMixareJSONObject(JSONObject jo) throws JSONException {

      return null;
    }

    public Marker processSNSJSONObject(JSONObject jo) throws JSONException {
        // TODO: 2016-06-03 SNS 내용 추가해야됨.
        Marker ma = null;
        String idStr = null;
        String snsLink = null;

        if(jo.has("longitude") && jo.has("latitude") && jo.has("time")) {

            idStr = jo.getString("id");
            snsLink = ("http://lab.khlug.org/manapie/javap/message.php?id=" + idStr);
            ma = new SnsMarker(jo.getString("name"),Double.parseDouble(jo.getString("latitude")),
                    Double.parseDouble(jo.getString("longitude")),0,snsLink,DataSource.DATASOURCE.SNS,jo.getString("message"),jo.getString("time"));

        }

        return ma;
    }


    // html 엔트리의 해쉬맵
    private static HashMap<String, String> htmlEntities;

    static {
        htmlEntities = new HashMap<String, String>();
        htmlEntities.put("&lt;", "<");
        htmlEntities.put("&gt;", ">");
        htmlEntities.put("&amp;", "&");
        htmlEntities.put("&quot;", "\"");
        htmlEntities.put("&agrave;", "à");
        htmlEntities.put("&Agrave;", "À");
        htmlEntities.put("&acirc;", "â");
        htmlEntities.put("&auml;", "ä");
        htmlEntities.put("&Auml;", "Ä");
        htmlEntities.put("&Acirc;", "Â");
        htmlEntities.put("&aring;", "å");
        htmlEntities.put("&Aring;", "Å");
        htmlEntities.put("&aelig;", "æ");
        htmlEntities.put("&AElig;", "Æ");
        htmlEntities.put("&ccedil;", "ç");
        htmlEntities.put("&Ccedil;", "Ç");
        htmlEntities.put("&eacute;", "é");
        htmlEntities.put("&Eacute;", "É");
        htmlEntities.put("&egrave;", "è");
        htmlEntities.put("&Egrave;", "È");
        htmlEntities.put("&ecirc;", "ê");
        htmlEntities.put("&Ecirc;", "Ê");
        htmlEntities.put("&euml;", "ë");
        htmlEntities.put("&Euml;", "Ë");
        htmlEntities.put("&iuml;", "ï");
        htmlEntities.put("&Iuml;", "Ï");
        htmlEntities.put("&ocirc;", "ô");
        htmlEntities.put("&Ocirc;", "Ô");
        htmlEntities.put("&ouml;", "ö");
        htmlEntities.put("&Ouml;", "Ö");
        htmlEntities.put("&oslash;", "ø");
        htmlEntities.put("&Oslash;", "Ø");
        htmlEntities.put("&szlig;", "ß");
        htmlEntities.put("&ugrave;", "ù");
        htmlEntities.put("&Ugrave;", "Ù");
        htmlEntities.put("&ucirc;", "û");
        htmlEntities.put("&Ucirc;", "Û");
        htmlEntities.put("&uuml;", "ü");
        htmlEntities.put("&Uuml;", "Ü");
        htmlEntities.put("&nbsp;", " ");
        htmlEntities.put("&copy;", "\u00a9");
        htmlEntities.put("&reg;", "\u00ae");
        htmlEntities.put("&euro;", "\u20a0");
    }

    // HTML 아스키 값들을 다시 복원. 변환할 소스와 시작점을 인자로 받는다
    public String unescapeHTML(String source, int start) {
        int i, j;    // 임시 변수

        // &와 ;의 위치로 값들을 읽는다
        i = source.indexOf("&", start);
        if (i > -1) {
            j = source.indexOf(";", i);
            if (j > i) {
                // 검색된 위치에서 값을 읽어옴
                String entityToLookFor = source.substring(i, j + 1);
                String value = (String) htmlEntities.get(entityToLookFor);

                // 값이 있을 시 복원작업 시작. 재귀호출 이용
                if (value != null) {
                    source = new StringBuffer().append(source.substring(0, i))
                            .append(value).append(source.substring(j + 1))
                            .toString();
                    return unescapeHTML(source, i + 1); // recursive call
                }
            }
        }
        return source;    // 복원된 소스 리턴
    }
}

