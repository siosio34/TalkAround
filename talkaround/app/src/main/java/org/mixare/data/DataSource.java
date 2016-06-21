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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.mixare.R;



// 데이터 소스를 실질적으로 다루는 클래스
public class DataSource {

    // 데이터 소스와 데이터 포맷은 비슷해 보이지만 전혀 다르다.
    // 데이터 소스는 데이터가 어디서 왔는지, 데이터 포맷은 어떤 형식으로 포맷되었는지를 가르킨다.
    // 이에 대한 이해는 똑같은 데이터 포맷으로 여러가지의 데이터 소스를 실험하는데에 필수적이다

    // 데이터 소스와 데이터 포맷의 열거형 변수
    public enum DATASOURCE {
        SCHOOL, SCHOOLRestaurant,BUSSTOP, CAFE, Convenience, Restaurant,SNS, NAVER
    };

    public enum DATAFORMAT {
        SCHOOL, SCHOOLRestaurant,BUSSTOP, CAFE ,Convenience,Restaurant,SNS, NAVER
    };

    public enum DATAFLAG {
        SCHOOL, SCHOOLRestaurant, CAFE, BUSSTOP, Convenience, Restaurant, SNS, ARTDESIGN, DORM, DORM2, ELECTRONIC, ENGINE, FORLANG, GATE, LIBRARY, LIFESCI, PHYSICAL, MANAGEINT, MULTI, STUDENT, VILIAGE, PANPACIFIC, BASIC
    }

    // 주의할것! 방대한 양의 데이터(MB단위 이상)을 산출할 때에는, 작은 반경이나 특정한 쿼리만을 사용해야한다
    /**
     * URL 부분 끝
     */

    // 아이콘들. 트위터와 버즈
    public static Bitmap twitterIcon;
    public static Bitmap buzzIcon;

    public static Bitmap schoolicon;
    public static Bitmap artdesignicon;
    public static Bitmap dormicon; // 우정원
    public static Bitmap dorm2icon; // 제이긱
    public static Bitmap elecicon;
    public static Bitmap forlangicon; //외국어대학
    public static Bitmap gateicon;
    public static Bitmap libraryicon;
    public static Bitmap lifesci;
    public static Bitmap manageinticon;
    public static Bitmap multi;
    public static Bitmap student;
    public static Bitmap viliage;
    public static Bitmap panpacific;
    public static Bitmap basic;
    public static Bitmap engine;
    public static Bitmap physical;

    public static Bitmap schoolRestaurant;
    public static Bitmap cafeIcon;
    public static Bitmap busIcon;
    public static Bitmap restraurantIcon;
    public static Bitmap bankIcon;
    public static Bitmap convenienceIcon;
    public static Bitmap routeIcon;
    public static Bitmap messeage_icon;

    public static Bitmap sns_add;

    //TODO : naver map URL
    private static final String NAVER_MAP_URL =		"http://map.naver.com/findroute2/findWalkRoute.nhn?call=route2&output=json&coord_type=naver&search=0";


    // TODO: 2016-05-31  위에 나머지 비트맵 이미지 넣어놓기. 
    // 기본 생성자
    public DataSource() {

    }

    // 리소스로부터 각 아이콘 생성
    public static void createIcons(Resources res) {
        //twitterIcon = BitmapFactory.decodeResource(res, R.drawable.twitter);
        //buzzIcon = BitmapFactory.decodeResource(res, R.drawable.coffee2);
        // schoolIcon ;
        // schoolRestaurant;
        cafeIcon = BitmapFactory.decodeResource(res, R.drawable.icon_cafe);
        messeage_icon = BitmapFactory.decodeResource(res,R.drawable.sns_2);
        busIcon = BitmapFactory.decodeResource(res,R.drawable.icon_metro);
        restraurantIcon = BitmapFactory.decodeResource(res,R.drawable.icon_store);
        convenienceIcon = BitmapFactory.decodeResource(res,R.drawable.icon_conveni);

        artdesignicon = BitmapFactory.decodeResource(res,R.drawable.school_artdesign);
        dormicon = BitmapFactory.decodeResource(res,R.drawable.school_dorm);
        dorm2icon = BitmapFactory.decodeResource(res,R.drawable.school_dorm2);
        elecicon = BitmapFactory.decodeResource(res,R.drawable.school_elecinfo);
        forlangicon = BitmapFactory.decodeResource(res,R.drawable.school_forlang);
        gateicon = BitmapFactory.decodeResource(res,R.drawable.school_gate);
        libraryicon = BitmapFactory.decodeResource(res,R.drawable.school_library);
        lifesci = BitmapFactory.decodeResource(res,R.drawable.school_lifesci);
        manageinticon = BitmapFactory.decodeResource(res,R.drawable.school_manageint);
        multi = BitmapFactory.decodeResource(res,R.drawable.school_multi);
        student = BitmapFactory.decodeResource(res,R.drawable.school_student);
        panpacific = BitmapFactory.decodeResource(res,R.drawable.school_panpacific);
        engine = BitmapFactory.decodeResource(res,R.drawable.school_engine);
        physical = BitmapFactory.decodeResource(res,R.drawable.school_physical);
        viliage = BitmapFactory.decodeResource(res,R.drawable.school_village);
        basic = BitmapFactory.decodeResource(res,R.drawable.school_default);
        sns_add = BitmapFactory.decodeResource(res,R.drawable.sns_add);
        // routeIcon;
    }

    // 아이콘 비트맵의 게터
    public static Bitmap getBitmap(String ds) {
        Bitmap bitmap = null;
        switch (ds) {

            // case "artdesign":
            //     bitmap = cafeIcon;
            //     break;
            // case SCHOOLRestaurant:
            //     bitmap = cafeIcon;
            //     break;
            case "artdesign":
                bitmap = artdesignicon;
                break;
            case "dorm":
                bitmap = dormicon;
                break;
            case "dorm2":
                bitmap = dorm2icon;
                break;
            case "elecinfo":
                bitmap = elecicon;
                break;
            case "forlang":
                bitmap = forlangicon;
                break;
            case "gate":
                bitmap = gateicon;
                break;
            case "library":
                bitmap = libraryicon;
                break;
            case "lifesci":
                bitmap = lifesci;
                break;
            case "manageint":
                bitmap = manageinticon;
                break;
            case "student":
                bitmap = multi;
                break;
            case "panpacific":
                bitmap = panpacific;
                break;
            case "engine":
                bitmap = engine;
                break;
            case "physical":
                bitmap = physical;
                break;
            case "default":
                bitmap = basic;
                break;
            case "CAFE":
                bitmap = cafeIcon;
                break;
            case "BUSSTOP":
                bitmap = busIcon;
                break;
            case "CONVENICE":
                bitmap = convenienceIcon;
                break;
            case "RESTRAUNT":
                bitmap = restraurantIcon;
                break;
            case "SNS":
                bitmap = messeage_icon;
                break;
            case "SNSADD":
                bitmap = sns_add;
                break;
            // TODO: 2016-05-31  여기에 케이스 더 다양하게해서 비트맵 파일을 받아놔야된다.

        }
        return bitmap;
    }

    // 데이터 소스로부터 데이터 포맷을 추출
    public static DATAFORMAT dataFormatFromDataSource(DATASOURCE ds) {
        DATAFORMAT ret;
        // 소스 형식에 따라 포맷을 할당한다
        switch (ds) {

            case SCHOOL:
                ret = DATAFORMAT.SCHOOL;
                break;

            case SCHOOLRestaurant:
                ret = DATAFORMAT.SCHOOLRestaurant;
                break;

            case BUSSTOP: // 버스 정류장
                ret = DATAFORMAT.BUSSTOP;
                break;

            case CAFE:
                ret = DATAFORMAT.CAFE;
                break;

            case Convenience:
                ret = DATAFORMAT.Convenience;
                break;

            case Restaurant:
                ret = DATAFORMAT.Restaurant;
                break;

            case SNS:
                ret = DATAFORMAT.SNS;
                break;
            case NAVER:
                ret=DATAFORMAT.NAVER;
                break;

            default:
                ret = DATAFORMAT.SCHOOL;
                break;


        }
        return ret;    // 포맷 리턴
    }

    //TODO : create naver map request url
    public static String createNaverMapRequestURL(double start_lon, double start_lat, double end_lon, double end_lat) {
        String ret = ""; // 결과 스트링
        ret = NAVER_MAP_URL;

        ret += "&start=" + Double.toString(start_lon) + "%2C" + Double.toString(start_lat)
                + "&destination=" + Double.toString(end_lon) + "%2C" + Double.toString(end_lat);

        return ret;
    }

    // 각 정보들로 완성된 URL 리퀘스트를 생성
    public static String createRequestURL(DATASOURCE source, double lat, double lon, double alt, float radius, String locale) {
        String ret = "";    // 결과 스트링

        // 파일로부터 읽는 것이 아니라면
        if (!ret.startsWith("file://")) {

            // 각 소스에 따른 URL 리퀘스트를 완성한다
            switch (source) {

                case SCHOOL:
                    ret = "http://lab.khlug.org/manapie/javap/getSchoolInfo.php";
                    break;

                case SCHOOLRestaurant:
                    ret = "http://lab.khlug.org/manapie/javap/getSchoolFood.php";
                    // TODO: 2016-05-31 학교식당 및 메뉴를 추가해야됨. 
                    break;

                case CAFE:
                    ret = "http://map.naver.com/search2/interestSpot.nhn?type=CAFE&boundary=" + Double.toString(lon - 0.02) + "%3B" +
                            Double.toString(lat - 0.01) + "%3B" + Double.toString(lon + 0.02) +
                            "%3B" + Double.toString(lat + 0.01) + "&pageSize=100";
                    break;

                case BUSSTOP:
                    ret = "http://map.naver.com/search2/searchBusStopWithinRectangle.nhn?bounds="+ Double.toString(lon - 0.02) + "%3B" +
                            Double.toString(lat - 0.01) +"%3B" +  Double.toString(lon + 0.02) + "%3B"
                            + Double.toString(lat + 0.01) +"&count=100&level12";
                    break;

                case Convenience:
                    ret = "http://map.naver.com/search2/interestSpot.nhn?type=STORE&boundary=" + Double.toString(lon - 0.02) + "%3B" +
                            Double.toString(lat - 0.01) + "%3B" + Double.toString(lon + 0.02) +
                            "%3B" + Double.toString(lat + 0.01) + "&pageSize=100";
                    break;

                case Restaurant:
                    ret =  "http://map.naver.com/search2/interestSpot.nhn?type=DINING_KOREAN&boundary=" + Double.toString(lon - 0.02) + "%3B" +
                            Double.toString(lat - 0.01) + "%3B" + Double.toString(lon + 0.02) +
                            "%3B" + Double.toString(lat + 0.01) + "&pageSize=100";
                    break;

                case SNS:
                    ret = "http://lab.khlug.org/manapie/javap/getMessage.php?longitude=" + Double.toString(lon) + "&latitude=" + Double.toString(lat);
                    break;








                // case OWNURL:
                //     //ret = "http://map.naver.com/search2/searchBusStopWithinRectangle.nhn?bounds=" +
                //     //        Double.toString(lon - 0.02) + "%3B" + Double.toString(lat - 0.01) +"%3B" +
                //     //        Double.toString(lon + 0.02) + "%3B" + Double.toString(lat + 0.01) +"%count=100&level11";
                //     //ret += "?type=CAFE&boundary=" + Double.toString(lon - 0.02) + "%3B" +
                //     //        Double.toString(lat - 0.01) + "%3B" + Double.toString(lon + 0.02) +
                //     //        "%3B" + Double.toString(lat + 0.01) + "&pageSize=100";
                //     ret = "http://map.naver.com/search2/interestSpot.nhn?type=CAFE&boundary=" + Double.toString(lon - 0.02) + "%3B" +
                //             Double.toString(lat - 0.01) + "%3B" + Double.toString(lon + 0.02) +
                //             "%3B" + Double.toString(lat + 0.01) + "&pageSize=100";
                //     break;

                // case ARRIVEBUS:
                //     ret = "http://m.gbis.go.kr/search/getStationPageList.do?keyword=29040";
                //     break;


            }

        }

        return ret;
    }

    // 각 소스에 따른 색을 리턴
    public static int getColor(DATASOURCE datasource) {
        int ret;
        switch (datasource) {
            //case BUZZ:		ret = Color.rgb(4, 228, 20); break;
            //case TWITTER:	ret = Color.rgb(50, 204, 255); break;
            //case OSM:		ret = Color.rgb(255, 168, 0); break;
            //case WIKIPEDIA:	ret = Color.RED; break;
            default:
                ret = Color.GREEN;
                break;
        }
        return ret;
    }

}
