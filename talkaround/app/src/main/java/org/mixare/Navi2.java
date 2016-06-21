package org.mixare;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by bonno22 on 2016-05-27.
 */
public class Navi2 implements Runnable {

    MixContext ctx;

    public static Toast mToast;

    private static final int MSG_TOAST_THREAD = 1;
    Handler handler = new Handler();

    Location loc;

    public float curDirection=0;
    public double startLat=0;
    public double  startLng=0;

    public double endLat=0;
    public double endLng=0;

    public double refdistance=0;//출발지와 도착지 간의 직선 거리. 그리고 이전 distance. 매번 갱신. 배열을 쓸까 생각중 distance와 curDistance를 비교해 반대 방향인지 판별
    public double curDistance=0;// 내 위치와 도착지 간의 직선 거리.gps 받을때마다 갱신,  점점 커지면 잘못가는 길(조금은 커질 수도 있다).

    public double tempDistance=0;// 구간 distance 비교, path[]값과
    public double tempLat=0;
    public double tempLng=0;

    public double curLong;//= 127.0813631;//gps에서 수시로 받아오는 값
    public double curLat;//= 37.2398864;

   // int[] path ={51,0,87,0,128,0,40,0,94,-1};
    int totalDistance=0;

    float[] distance=new float[2];
    //int[] num={};// 횡단보도 번호를 알아내기 위한 것 , 횡단보도는 no 8이며 거리가 있지만 distance 0으로 되어 있다
   // String[] pathName = {"엣코너까지 약 13m 이동(영일로21번길)", "오른쪽",
   //         "영일중학교까지 약 69m 이동(영일로21번길)", "왼쪽", "피자에땅영통점깢지 약 36m이동(매영로393번길)",
   //         "피자에땅영통정"};

   //String[] pathName = {"약 51m 이동", "왼쪽",
   //        "약 87m 이동", "오른쪽", "약128m 이동",
   //        "왼쪽","약 40m 이동","오른쪽","경희대학교 국제캠퍼스 공과대학관까지 약 94m 이동","경희대학교 국제 캠퍼스 공과대학관"};

    double[] path;
    String[] pathName;

    MyHandler m_handler= new MyHandler();
    String curpath;

    public static boolean isStart=false;
    public static boolean isFinish=false;
    public Navi2(MixContext ctx){ this.ctx=ctx; mToast = Toast.makeText(ctx, "null", Toast.LENGTH_SHORT); }
    public static List<Marker> pathMarkerList;
    public void run() {


        while(true) {
            if (isStart) {
                path = new double[pathMarkerList.size()];
                pathName = new String[pathMarkerList.size()];


                for(int i=0; i<pathMarkerList.size(); i++){
                    path[i]=pathMarkerList.get(i).getDistance();
                    pathName[i]=pathMarkerList.get(i).getDescription();

                    Log.i("path",String.valueOf(path[i]));
                    Log.i("Pathname",String.valueOf(path[i]));

                }
             //   path[pathMarkerList.size()-1]=-1;
                //초기값 세팅
                int pathIndex = 1;
                int pathNameIndex = 1;
                boolean reach = false;

                String Direction = "";//파싱한거를 오른쪽 왼쪽 받는다.
                double dummy=0;
                int temp = 1;

                float refDirection = 0;//방향 toast가 떴을떄 내가 보는 방향 바뀌지 않음
                float subDirection = 0;//돌아간 방향의 정도
                boolean IsTurn = false;


                while (temp < path.length) {
                    if (path[pathIndex] == 0) {
                        refDirection = curDirection;//지금 보고 있는 방향 저장
                        Direction = pathName[pathNameIndex];//오른쪽 왼쪽

                        IsTurn=false;
                        //    m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, curpath));
                        while (IsTurn==false) {
                            //방향을 설명을 계속한다. 다 돌때까지
                           // refDirection = curDirection;
                            curpath = Direction + "으로 도세요";
                            //  Log.v("알림", curpath);
                            m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, curpath));
                           sleep(2000);
                            //subDirction 이 -로 증가하면 오른쪽으로 회전, +로 증가하면 왼쪽으로 회전
                            subDirection = refDirection - curDirection;
                          //  Log.v("알림", Direction + "회전 각 : " + subDirection);

                            Log.v("알림","ref : "+refDirection+ " cur : "+curDirection+"sub : "+subDirection);
                        //    Log.v("알림","ref : "+refDirection+ " cur : "+curDirection);
                            if (subDirection < -50) {
                                pathIndex++;
                                pathNameIndex++;
                                IsTurn = true;
                                Log.v("알림", "오른쪽으로 돌았습니다.");
                                break;
                            } else if (subDirection > 50) {
                                pathIndex++;
                                pathNameIndex++;
                                IsTurn = true;
                                Log.v("알림", "왼쪽으로 돌았습니다.");
                                break;
                            }
                            //방향이랑 회전이랑 안맞을시 예외처리
                            else {

                            }
                        }
                        curpath = "";
                        refDirection = 0;
                        subDirection = 0;
                        Direction = "";
               //         IsTurn = false;
                        curDirection=0;
                        temp++;

                    }else if(path[pathIndex] == -1){
                        Log.v("Navi2", " path :" + curpath + "도착");
                        m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, " path :" + curpath + "도착"));
                        isStart=false;
                        break;
                    }
                    else {//직선 거리를 가야하는 것
                        tempLat = curLat;
                        tempLng = curLong;//좌표 저장(ref좌표) 이 좌표 값들이랑 갱신되는 cur 좌표값 사이의 거리를 가져온다.
                        curpath = "";
                        curpath = pathName[pathNameIndex];
                        //  m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, curpath));
                        while (tempDistance < path[pathIndex] - 8) {
                           Location.distanceBetween(tempLat, tempLng, curLat, curLong, distance);
                            tempDistance = distance[0];
                            //tempDistance구하기 tempLat,lng과 curLat,lng사이
                            // toast pathName[pathNameIndex] 같은 값을 반복해줌  일직선이니깐

                           //tempDistance+=10;
                            dummy=path[pathIndex]-tempDistance;
                            Log.v("Navi2", tempDistance + " path :" + curpath);
                            m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, "앞으로 가세요. 약 " + dummy+"m 남았습니다."));
                            sleep(2000);

                        }
                        tempDistance = 0;//초기화
                        curpath = "";
                        pathIndex++;
                        pathNameIndex++;
                        temp++;
                    }
                }
                m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, "공과대학관에 도착하셨습니다"));
                sleep(1000);
                m_handler.sendMessage(m_handler.obtainMessage(MSG_TOAST_THREAD, "공과대학관에 도착하셨습니다"));
                Log.v("Navi2",path[pathNameIndex-1]+"에 도착하셨습니다");
                sleep(2000);
                isStart=false;
                isFinish=true;
            }
        }
    }
    public void setGPSLatLng(double lat,double lng)
    {
        //distance에 원래 거리를 넣고 들어온 값 세팅

        curLat=lat;
        curLong=lng;

        //cur distance에 세팅 된 값 넣기
    }

    public void setOrientation(float x){
        curDirection=x;
    }

    private void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (java.lang.InterruptedException ex) {

        }
    }
    class MyHandler extends Handler{

      //  Context m_ctx;
      //  MyHandler(MixContext m_ctx){this.m_ctx=m_ctx;}
        public void handleMessage(Message msg){
            switch(msg.what) {
                case MSG_TOAST_THREAD:

                    mToast.setText((String) msg.obj);
                    mToast.show();
            }
        }
    }
}

