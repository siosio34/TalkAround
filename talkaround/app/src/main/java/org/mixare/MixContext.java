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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

//adding support for https connections
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


import org.mixare.data.DataSource;
import org.mixare.data.DataSource.DATASOURCE;
import org.mixare.render.Matrix;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

// 컨텍스트랩퍼를 확장하는 컨텍스트 클래스
public class MixContext extends ContextWrapper {

    // 뷰와 컨텍스트
    public MixView mixView;
    Context ctx;

    boolean isURLvalid = true;    // URL이 유효한지 여부
    Random rand;    // 랜덤 수치를 생성하기 위함

    DownloadManager downloadManager;    // 다운로드 관리자

    Location curLoc;    // 현재 위치
    Location locationAtLastDownload;    // 마지막으로 다운로드된 위치
    Matrix rotationM = new Matrix();    // 회전연산에 사용될 행렬

    float declination = 0f;    // 경사, 적위
    private boolean actualLocation = false;

    LocationManager locationMgr;        // 위치 관리자

    // 각 데이터소스의 선택 여부를 저장할 해쉬맵
    private HashMap<DataSource.DATASOURCE, Boolean> selectedDataSources = new HashMap<DataSource.DATASOURCE, Boolean>();

    // 생성자. 어플리케이션의 컨텍스트를 받는다
    public MixContext(Context appCtx) {
        super(appCtx);

        // 메인 뷰와 컨텍스트를 할당
        this.mixView = (MixView) appCtx;
        this.ctx = appCtx.getApplicationContext();

        // 액티비티의 자체 세팅을 공유할 프레퍼런스
        SharedPreferences settings = getSharedPreferences(MixView.PREFS_NAME, 0);
        boolean atLeastOneDatasourceSelected = false;    // 최소 하나 이상의 데이터 소스가 선택되었는지 여부

        // 데이터 소스 전체를 돌며 적용
        for (DataSource.DATASOURCE source : DataSource.DATASOURCE.values()) {
            // 선택된 데이터소스의 해쉬맵을 프레퍼런스 세팅값에 따라 설정
            selectedDataSources.put(source, settings.getBoolean(source.toString(), false));
            // 한개라도 선택된 것이 있다면 플래그를 true
            if (selectedDataSources.get(source))
                atLeastOneDatasourceSelected = true;
        }
        // 아무것도 선택된 것이 없을 경우 위키피디아를 선택한다
        // (위키피디아는 기본 데이터 소스로 한다)

        // TODO: 2016-05-31  이건 나중에 학교로 변경하도록하자
        if (!atLeastOneDatasourceSelected)
            setDataSource(DATASOURCE.CAFE, true);

        // 가장 기본


        // 회전행렬을 일단 단위행렬로 세팅
        rotationM.toIdentity();

        int locationHash = 0;    // 위치 해쉬값

        try {
            // 메인 컨텍스트의 위치 제공자로부터 위치 관리자 등록
            locationMgr = (LocationManager) appCtx.getSystemService(Context.LOCATION_SERVICE);

            // GPS 로부터 마지막으로 선택된 위치값을 등록
            Location lastFix = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // 설정된 위치값이 없다면 네트워크로부터 마지막으로 선택된 위치값을 등록
            if (lastFix == null) {
                lastFix = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // lastFix 값이 할당되었을 경우
            if (lastFix != null) {
                // 위치 해쉬값을 이용하여
                locationHash = ("HASH_" + lastFix.getLatitude() + "_" + lastFix.getLongitude()).hashCode();

                // 실 시간과 시차 등을 계산
                long actualTime = new Date().getTime();
                long lastFixTime = lastFix.getTime();
                long timeDifference = actualTime - lastFixTime;

                actualLocation = timeDifference <= 1200000;    //20 min --- 300000 milliseconds = 5 min
            } else
                actualLocation = false;


        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 계산된 위치 해쉬값으로 랜덤값 발생
        rand = new Random(System.currentTimeMillis() + locationHash);
    }

    // 현재의 GPS 정보를 리턴. 위치 관리자로부터 현재의 위치를 반환한다
    public Location getCurrentGPSInfo() {
        return curLoc != null ? curLoc : locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    // GPS 사용 가능 여부 리턴
    public boolean isGpsEnabled() {
        return mixView.isGpsEnabled();
    }

    // 정확한 위치가 맞는지 리턴
    public boolean isActualLocation() {
        return actualLocation;
    }

    // 사용중인 다운로드 관리자 리턴
    public DownloadManager getDownloader() {
        return downloadManager;
    }

    // 위치 관리자를 지정
    public void setLocationManager(LocationManager locationMgr) {
        this.locationMgr = locationMgr;
    }

    // 사용중인 위치 관리자를 리턴
    public LocationManager getLocationManager() {
        return locationMgr;
    }

    // 시작 Url 경로를 리턴한다
    public String getStartUrl() {
        return "";
    }

    // 인자로 받는 dest 에 회전 행렬을 세팅
    public void getRM(Matrix dest) {
        synchronized (rotationM) {
            dest.set(rotationM);
        }
    }

    // 현재의 위치를 리턴
    public Location getCurrentLocation() {
        synchronized (curLoc) {
            return curLoc;
        }
    }

    // GET 형식으로 데이터를 받아 인풋 스트림을 리턴한다
    public InputStream getHttpGETInputStream(String urlStr)
            throws Exception {
        InputStream is = null;    // 내용을 읽어올 인풋 스트림
        URLConnection conn = null;    // URL 과의 통신을 위한 URLConnection 객체

        // 각 파일, 컨텐트, 네트워크 주소등에 따른 스트림을 읽을 준비
        if (urlStr.startsWith("file://"))
            return new FileInputStream(urlStr.replace("file://", ""));

        if (urlStr.startsWith("content://"))
            return getContentInputStream(urlStr, null);

        // 네트워크 부분은 절차가 좀 복잡하다(SSL/TLS)
        if (urlStr.startsWith("https://")) {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            // SSL 통신용 컨텍스트
            SSLContext context = SSLContext.getInstance("TLS");

            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        }

        try {
            URL url = new URL(urlStr);    // 준비된 스트링 값으로 URL 을 생성
            // 커넥션 설정을 한다
            conn = url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            is = conn.getInputStream();    // 인풋 스트림으로 저장하여 리턴

            return is;
        } catch (Exception ex) {    // 예외 처리
            try {
                is.close();    // 인풋 스트림을 닫고
            } catch (Exception ignore) {
            }
            try {    // 접속을 끊는다
                if (conn instanceof HttpURLConnection)
                    ((HttpURLConnection) conn).disconnect();
            } catch (Exception ignore) {
            }

            throw ex;

        }
    }

    // 네트워크의 데이터를 인풋 스트림을 스트링 형태로 리턴
    public String getHttpInputString(InputStream is) {
        // 인풋 스트림으로부터 데이터를 읽을 버퍼와 그에 사용될 스트링 빌더
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8 * 1024);
        StringBuilder sb = new StringBuilder();

        try {
            // 행 단위로 읽어 뒤에 개행코드를 추가한다
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {    // 모든 작업이 끝나면
            try {
                is.close();    // 스트림을 닫는다
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();    // 완성된 스트링을 리턴
    }

    // POST 형식으로 데이터를 받아 인풋 스트림을 리턴한다
    public InputStream getHttpPOSTInputStream(String urlStr,
                                              String params) throws Exception {

        // 사용될 인풋, 아웃풋 스트림과 커넥션 객체
        InputStream is = null;
        OutputStream os = null;
        HttpURLConnection conn = null;

        // 컨텐트의 경우
        if (urlStr.startsWith("content://"))
            return getContentInputStream(urlStr, params);

        try {
            URL url = new URL(urlStr);    // 준비된 스트링으로 URL 생성
            // 커넥션 설정
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            // 파라메터 값이 null 이 아닐 때
            if (params != null) {
                conn.setDoOutput(true);
                os = conn.getOutputStream();    // 커넥션 객체로부터 아웃풋 스트림을 읽고
                OutputStreamWriter wr = new OutputStreamWriter(os);    // 라이터를 생성
                wr.write(params);    // 파라메터에 기록
                wr.close();
            }

            is = conn.getInputStream();    // 커넥션 객체로부터 인풋 스트림을 읽어옴

            return is;    // 읽어온 인풋 스트림을 리턴
        } catch (Exception ex) {
            // 예외 처리
            try {
                is.close();
            } catch (Exception ignore) {

            }
            try {
                os.close();
            } catch (Exception ignore) {

            }
            try {
                conn.disconnect();
            } catch (Exception ignore) {
            }

            // 405 에러 시에는 GET형식으로
            if (conn != null && conn.getResponseCode() == 405) {
                return getHttpGETInputStream(urlStr);
            } else {

                throw ex;
            }
        }
    }

    // 컨텐트 인풋 스트림을 리턴
    public InputStream getContentInputStream(String urlStr, String params)
            throws Exception {
        // 쿼리를 통해 컨텐트 프로바이더(CP)와 통신할 ContentResolver 객체와 커서
        ContentResolver cr = mixView.getContentResolver();
        // ContentResolver 의 쿼리를 통해 urlStr 을 파싱하여 커서를 생성한다. 파라메터 이용
        Cursor cur = cr.query(Uri.parse(urlStr), null, params, null, null);

        cur.moveToFirst();    // 커서를 맨 처음으로 옮기고, 모드를 읽어 저장
        int mode = cur.getInt(cur.getColumnIndex("MODE"));

        // 모드가 1일 경우
        if (mode == 1) {
            // 결과를 읽는다
            String result = cur.getString(cur.getColumnIndex("RESULT"));
            cur.deactivate();

            // 결과를 바이트단위의 인풋스트림으로 변환하여 리턴한다
            return new ByteArrayInputStream(result
                    .getBytes());
        } else {
            cur.deactivate();

            // 다른 모드일 경우엔 예외 발생
            throw new Exception("Invalid content:// mode " + mode);
        }
    }

    // 네트워크 인풋 스트림을 닫는다
    public void returnHttpInputStream(InputStream is) throws Exception {
        if (is != null) {
            is.close();
        }
    }

    // 리소스 인풋 스트림을 리턴
    public InputStream getResourceInputStream(String name) throws Exception {
        AssetManager mgr = mixView.getAssets();    // assets 안의 파일을 접근하기 위함
        return mgr.open(name);
    }

    // 리소스 인풋 스트림을 닫는다
    public void returnResourceInputStream(InputStream is) throws Exception {
        if (is != null)
            is.close();
    }

    // 웹페이지를 로드
            public void loadMixViewWebPage(String url) throws Exception {
                // TODO
                WebView webview = new WebView(mixView);    // 웹 뷰
                webview.getSettings().setJavaScriptEnabled(true);    // 자바스크립트 허용
                webview.getSettings().setDomStorageEnabled(true);
                // URL 을 연결하여 웹 뷰 클라이언트를 세팅
                webview.setWebViewClient(new WebViewClient() {
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
            }

        });

        // 다이얼로그를 생성
        Dialog d = new Dialog(mixView) {
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    this.dismiss();
                return true;
            }
        };

        // 웹 뷰를 다이얼로그 연결한다
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setGravity(Gravity.BOTTOM);
        d.addContentView(webview, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        d.show();    // 다이얼로그 출력

        webview.loadUrl(url);    // 웹 뷰에 url 로드
    }

    // 웹 페이지 로드. 위 메소드와의 차이는 컨텍스트를 별도로 지정한다는 것이다
    public void loadWebPage(String url, Context context) throws Exception {
        // TODO
        WebView webview = new WebView(context);    // 웹 뷰

        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

        });

        Dialog d = new Dialog(context) {
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    this.dismiss();
                return true;
            }
        };
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setGravity(Gravity.BOTTOM);
        d.addContentView(webview, new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        d.show();

        webview.loadUrl(url);
    }


       // 데이터 소스 세팅
    // TODO: 2016-05-31 데이터 소스 선택시 여기서 아마 전체환경에 영향을 주는것 같다. 이거 아마 xml 내용 바꾸는듯
    public void setDataSource(DataSource.DATASOURCE source, Boolean selection) {
        selectedDataSources.put(source, selection);    // 선택된 데이터 소스의 상태를 세팅

        // 변경된 사항을 프레퍼런스에 세팅하고 적용한다
        SharedPreferences settings = getSharedPreferences(MixView.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(source.toString(), selection);
        editor.commit();
    }

    // 특정 데이터 소스가 선택된 상태인지 리턴
    public Boolean isDataSourceSelected(DataSource.DATASOURCE source) {
        return selectedDataSources.get(source);
    }

    // 데이터 소스의 선택 여부를 토글
    public void toogleDataSource(DataSource.DATASOURCE source) {
        setDataSource(source, !selectedDataSources.get(source));
    }

    // 선택된 데이터 소스 리스트를 스트링 형태로 리턴
    public String getDataSourcesStringList() {
        String ret = "";    // 결과 스트링
        boolean first = true;    // 첫번째 항목인지 여부(쉼표 찍기위해 구분)
        // 데이터 소스들을 점검
        for (DataSource.DATASOURCE source : DataSource.DATASOURCE.values()) {
            if (isDataSourceSelected(source)) {    // 선택된 경우 결과에 추가
                if (!first) {
                    ret += ", ";
                }
                ret += source.toString();
                first = false;
            }
        }
        return ret;    // 결과 스트링을 리턴
    }

    // 마지막으로 다운로드된 위치를 리턴
    public Location getLocationAtLastDownload() {
        return locationAtLastDownload;
    }

    // 마지막으로 다운로드된 위치를 세팅
    public void setLocationAtLastDownload(Location locationAtLastDownload) {
        this.locationAtLastDownload = locationAtLastDownload;
    }

}
