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

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.Json;
import org.mixare.data.XMLHandler;
import org.mixare.data.DataSource.DATAFORMAT;
import org.mixare.data.DataSource.DATASOURCE;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import android.util.Log;

// 다운로드 관리자 클래스. 스레드로 관리
public class DownloadManager implements Runnable {

	// 중단, 중지, 수행 여부
	private boolean stop = false, pause = false, proceed = false;
	// 사용될 상수들. 0~4 까지 각각 미시작, 접속중, 접속됨, 중지됨, 중단됨
	public static int NOT_STARTED = 0, CONNECTING = 1, CONNECTED = 2, PAUSED = 3, STOPPED = 4;
	private int state = NOT_STARTED;	// 현재 상태는 일단 미시작

	private int id = 0;	// 다운로드 항목의 ID
	
	// 다운로드 요청항목과 완료항목의 해쉬맵
	private HashMap<String, DownloadRequest> todoList = new HashMap<String, DownloadRequest>();
	private HashMap<String, DownloadResult> doneList = new HashMap<String, DownloadResult>();
	InputStream is;	// 다운로드에 사용될 인풋 스트림

	private String currJobId = null;	// 현재 수행 항목의 ID

	MixContext ctx;	// 다운로드 관리자의 컨텍스트

	
	// 생성자. 컨텍스트를 할당
	public DownloadManager(MixContext ctx) {
		this.ctx = ctx;
	}

	// 스레드 수행
	public void run() {
		String jobId;	// 처리중인 작업의 ID

		// * 다운로드에 대한 객체들의 클래스는 하단에서 정의한다  
		DownloadRequest request;	// 다운로드 요청
		DownloadResult result;		// 다운로드 결과(완료)

		// 일단 모든 플래그를 false, 상태를 접속중으로 바꾼다
		stop = false;
		pause = false;
		proceed = false;
		state = CONNECTING;

		// stop 플래그가 false 일 때
		while (!stop) {
			// ID와 요청, 결과를 일단 null 로 세팅한다
			jobId = null;
			request = null;
			result = null;

			// 수행을 기다림
			while (!stop && !pause) {
				synchronized (this) {
					if (todoList.size() > 0) {	// 받아야 할 항목이 있을 경우
						jobId = getNextReqId();	// 다음 ID로 넘겨가며
						request = todoList.get(jobId);	// 리스트로부터 항목을 가져온다
						proceed = true;			// 수행중 플래그 true
					}
				}
				// 다운로드 수행
				if (proceed) {
					state = CONNECTED;	// 상태는 접속됨
					currJobId = jobId;	// 현재 ID를 처리할 ID로

					// 다운로드 결과에 요청된 자료를 넣는다 
					result = processRequest(request);

					// 요청 리스트에서 빼고 완료된 리스트에 ID와 결과를 삽입한다
					synchronized (this) {
						todoList.remove(jobId);
						doneList.put(jobId, result);
						proceed = false;	// 수행 완료
					}
				}
				state = CONNECTING;	// 상태는 여전히 접속 중

				if (!stop && !pause)
					sleep(100);		// 추가 작업이 있을 경우 약간의 딜레이를 준다
			}

			// 중단 된 것은 아니나 중지 중일 때
			while (!stop && pause) {
				state = PAUSED;		// 상태는 중지
				sleep(100);			// 딜레이 발생
			}
			state = CONNECTING;
		}
		// 작업이 없고 중단 상태일때 상태를 변경
		state = STOPPED;
	}

	// 현재 접속상태를 리턴
	public int checkForConnection(){
		return state;
	}

	// 스레드에 딜레이를 줌(1/1000초 단위)
	private void sleep(long ms){
		try {
			Thread.sleep(ms);
		} catch (java.lang.InterruptedException ex) {

		}
	}

	// 다음 요청 ID를 리턴. 이터레이터 사용 
	private String getNextReqId() {
		return todoList.keySet().iterator().next();
	}

	// 수행 요청
	private DownloadResult processRequest(DownloadRequest request) {
		DownloadResult result = new DownloadResult();
		// 모든것이 정상이라고 판단되기 전까지는 에러 상태로 본다
		result.error = true;
		
		try {
			// 컨텍스트, 리퀘스트가 있고 리퀘스트의 url 이 존재할 때 
			if(ctx!=null && request!=null && ctx.getHttpGETInputStream(request.url)!=null){

				is = ctx.getHttpGETInputStream(request.url);	// 인풋 스트림에 할당
				String tmp = ctx.getHttpInputString(is);	// 스트링 형태로 변환

				Json layer = new Json();	// JSON 파일을 다룰 객체

				// JSON 데이터를 로드한다
				try {

					Log.v(MixView.TAG, "try to load JSON data");

					// JSON 형태의 스트링으로 JSON 객체를 생성
					JSONObject root = new JSONObject(tmp);

					Log.d(MixView.TAG, "loading JSON data");				

					// JSON 객체와 포맷으로 마커를 생성한다
					List<Marker> markers = layer.load(root,request.format);
					result.setMarkers(markers);	// 다운로드 결과에 마커를 할당

					// 인자로 받은 리퀘스트로부터 포맷과 소스를 할당한다
					result.format = request.format;
					result.source = request.source;
					result.error = false;
					result.errorMsg = null;

				}
				catch (JSONException e) {
					// 예외 발생시. JSON 데이터가 아니라 판단하고 XML로 읽는다
					Log.v(MixView.TAG, "no JSON data");
					Log.v(MixView.TAG, "try to load XML data");

					try {
						// XML 파싱에 사용될 도큐먼트 빌더
						DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

						// 스트링 형태로 변환된 스트림을 빌더를 이용해 파싱한다
						Document doc = builder.parse(new InputSource(new StringReader(tmp)));
						//Document doc = builder.parse(is);
						//Document doc = builder.parse(is);

						XMLHandler xml = new XMLHandler();	// XML을 다룰 객체

						Log.i(MixView.TAG, "loading XML data");	
						
						// 도큐먼트로부터 파싱된 XML을 읽어 마커를 생성한다
						List<Marker> markers = xml.load(doc);
						result.setMarkers(markers);	// 다운로드 결과에 마커를 할당

						// 인자로 받은 리퀘스트로부터 포맷과 소스를 할당한다
						result.format = request.format;
						result.error = false;
						result.errorMsg = null;
					} catch (Exception e1) {
						e1.printStackTrace();
					}				
				}
				ctx.returnHttpInputStream(is);	// 인풋 스트림을 돌려보냄(닫음)
				is = null;
			}
		}
		catch (Exception ex) {	// 예외 발생시에는 에러 처리를 하고
			result.errorMsg = ex.getMessage();
			result.errorRequest = request;	// 에러가 발생한 리퀘스트에 등록

			try {	// 인풋 스트림을 닫는다
				ctx.returnHttpInputStream(is);
			} catch (Exception ignore) {
			}

			ex.printStackTrace();
		}

		currJobId = null;	// 모든 작업이 완료되면 작업중 ID를 null 로

		return result;	// 다운로드 결과를 반환한다
	}

	// 할당되어 있는 리스트들(요청, 완료)의 내용을 클리어 
	public synchronized void purgeLists() {
		todoList.clear();
		doneList.clear();
	}

	// 작업을 제출. 수행해야 할 작업 리스트에 등록한다
	public synchronized String submitJob(DownloadRequest job) {
		if(job!=null) {
			String jobId = "ID_" + (id++);	// ID를 할당. 'ID_번호' 형태이다
			todoList.put(jobId, job);	// ID와 함께 작업을 요청 리스트에 등록
			Log.i(MixView.TAG,"Submitted Job with "+jobId+", format: " +job.format+", params: "+job.params+", url: "+job.url);
			return jobId;	// 할당된 ID를 리턴한다
		}
		return null;
	}

	// 특정 ID의 작업이 완료되었는지 여부를 리턴
	public synchronized boolean isReqComplete(String jobId) {
		return doneList.containsKey(jobId);
	}

	// 특정 ID의 완료된 작업을 리턴
	public synchronized DownloadResult getReqResult(String jobId) {
		DownloadResult result = doneList.get(jobId);	// 완료 리스트로부터 읽는다
		doneList.remove(jobId);		// 꺼낸 항목은 제거

		return result;	// 결과를 리턴
	}

	// 현재 수행중인 작업의 ID를 리턴
	public String getActiveReqId() {
		return currJobId;
	}
	
	// 다운로드 스레드를 중지
	public void pause() {
		pause = true;
	}

	// 중지 상태를 풀어 작업 재개
	public void restart() {
		pause = false;
	}

	// 다운로드 스레드 작업 중단
	public void stop() {
		stop = true;
	}
	
	// 완료된 다음 작업을 리턴
	public synchronized DownloadResult getNextResult() {
		// 완료 리스트에 항목이 남아있을 경우
		if(!doneList.isEmpty()) {
			// 순서대로 항목을 리턴한다
			String nextId = doneList.keySet().iterator().next();
			DownloadResult result = doneList.get(nextId);
			doneList.remove(nextId);	// 리턴할 항목은 리스트에서 제거된다
			return result;
		}
		return null;
	}
	
	// 요청 작업이 남아있는지 여부 리턴
	public Boolean isDone() {
		return todoList.isEmpty();
	}
}

// 다운로드 요청 클래스
class DownloadRequest {
	// 데이터 포맷과 소스, 연결될 url, 그리고 파라메터 값들을 지닌다
	public DATAFORMAT format;
	public DATASOURCE source;
	String url;
	String params;
}

// 다운로드 결과 클래스
class DownloadResult {
	// 포맷과 소스
	public DATAFORMAT format;
	public DATASOURCE source;
	
	List<Marker> markers;	// 결과로부터 생성된 마커	

	// 에러 처리시 사용될 변수들
	boolean error;
	String errorMsg;
	DownloadRequest errorRequest;
	
	// 마커를 리턴
	public List<Marker> getMarkers() {
		return markers;
	}
	
	// 마커를 할당
	public void setMarkers(List<Marker> markers) {
		this.markers = markers;
	}
	
}
