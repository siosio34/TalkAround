package org.mixare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapCircleData;
import com.nhn.android.maps.overlay.NMapCircleStyle;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.maps.overlay.NMapPathData;
import com.nhn.android.maps.overlay.NMapPathLineStyle;
import com.nhn.android.mapviewer.overlay.NMapCalloutCustomOverlay;
import com.nhn.android.mapviewer.overlay.NMapCalloutOverlay;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.nhn.android.mapviewer.overlay.NMapPathDataOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.DataSource;
import org.mixare.data.Json;

import java.util.List;

/**
 * Created by PNY on 2016-05-22.
 */
public class MixNaverMap extends NMapActivity{

    //Context
    private Context thisContext;

    //Naver API
    private static final String LOG_TAG = "NMapViewer";
    private static final boolean DEBUG = false;

    private static final String CLIENT_ID = "D_t_01jivE_YOCXDS0eW";

    private MapContainerView mMapContainerView;

    private NMapView mMapView = null;
    private NMapController mMapController = null;

    //TODO : 넘어온 mixare marker 정보 저장
    private String markerListJson = "";

    private NGeoPoint passedMarkerGeoPoint = null;
    private String passedMarkerTitle = null;
    private Marker passedMarker = null;
    private List<Marker> pathMarkerList = null;

    private static final NGeoPoint NMAP_PATHSTART = new NGeoPoint(127.0767427, 37.2480001);
    private static final NGeoPoint NMAP_PATHEND = new NGeoPoint(127.0725230, 37.2517621);

    //TODO : for naver map request url
    //DownloadManager downloadManager = null;
    private static DataView dataView;
    private MixContext mixContext;
    private Thread downloadThread;
    private HttpConnection httpConnection = null;


    private static final NGeoPoint NMAP_LOCATION_DEFAULT = new NGeoPoint(126.978371, 37.5666091);
    private static final int NMAP_ZOOMLEVEL_DEFAULT = 11;
    private static final int NMAP_VIEW_MODE_DEFAULT = NMapView.VIEW_MODE_VECTOR;
    private static final boolean NMAP_TRAFFIC_MODE_DEFAULT = false;
    private static final boolean NMAP_BICYCLE_MODE_DEFAULT = false;

    private static final String KEY_ZOOM_LEVEL = "NMapViewer.zoomLevel";
    private static final String KEY_CENTER_LONGITUDE = "NMapViewer.centerLongitudeE6";
    private static final String KEY_CENTER_LATITUDE = "NMapViewer.centerLatitudeE6";
    private static final String KEY_VIEW_MODE = "NMapViewer.viewMode";
    private static final String KEY_TRAFFIC_MODE = "NMapViewer.trafficMode";
    private static final String KEY_BICYCLE_MODE = "NMapViewer.bicycleMode";

    private SharedPreferences mPreferences;

    private NMapOverlayManager mOverlayManager;

    private NMapMyLocationOverlay mMyLocationOverlay;
    private NMapLocationManager mMapLocationManager;
    private NMapCompassManager mMapCompassManager;

    private NMapViewerResourceProvider mMapViewerResourceProvider;

    private NMapPOIdataOverlay mFloatingPOIdataOverlay;
    private NMapPOIitem mFloatingPOIitem;

    private static boolean USE_XML_LAYOUT = true;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO : mixview, 메인 컨텍스트를 읽어옴
        dataView = MixView.dataView;
        mixContext = dataView.getContext();
        thisContext = mixContext;

        if (USE_XML_LAYOUT) {
            setContentView(R.layout.map);

            mMapView = (NMapView)findViewById(R.id.nmapview);
        } else {
            // create map view
            mMapView = new NMapView(this);

            // create parent view to rotate map view
            mMapContainerView = new MapContainerView(this);
            mMapContainerView.addView(mMapView);

            // set the activity content to the parent view
            setContentView(mMapContainerView);
        }

        // set a registered Client Id for Open MapViewer Library
        mMapView.setClientId(CLIENT_ID);

        // initialize map view
        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setFocusable(true);
        mMapView.setFocusableInTouchMode(true);
        mMapView.requestFocus();

        // register listener for map state changes
        mMapView.setOnMapStateChangeListener(onMapViewStateChangeListener);
        mMapView.setOnMapViewTouchEventListener(onMapViewTouchEventListener);
        mMapView.setOnMapViewDelegate(onMapViewTouchDelegate);

        // use map controller to zoom in/out, pan and set map center, zoom level etc.
        mMapController = mMapView.getMapController();

        // use built in zoom controls
        NMapView.LayoutParams lp = new NMapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, NMapView.LayoutParams.BOTTOM_RIGHT);
        mMapView.setBuiltInZoomControls(true, lp);

        // create resource provider
        mMapViewerResourceProvider = new NMapViewerResourceProvider(this);

        // set data provider listener
        super.setMapDataProviderListener(onDataProviderListener);

        // create overlay manager
        mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);
        // register callout overlay listener to customize it.
        mOverlayManager.setOnCalloutOverlayListener(onCalloutOverlayListener);
        // register callout overlay view listener to customize it.
        mOverlayManager.setOnCalloutOverlayViewListener(onCalloutOverlayViewListener);

        // location manager
        mMapLocationManager = new NMapLocationManager(this);
        mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);

        // compass manager
        mMapCompassManager = new NMapCompassManager(this);

        // create my location overlay
        mMyLocationOverlay = mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);

        //TODO : set passed marker from mixare
        //if(getIntent().getExtras() != null) {
        if(getIntent().getStringExtra("markerListJson") != null) {
            markerListJson = getIntent().getStringExtra("markerListJson");
            JSONArray markerList = new JSONArray();
            try {
                markerList = new JSONArray(markerListJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            drawPassedMarker(markerList);
        }

        if(getIntent().getExtras().getString("marker_title") != null) {
            passedMarkerGeoPoint = new NGeoPoint(getIntent().getExtras().getDouble("longitude"), getIntent().getExtras().getDouble("latitude"));
            passedMarkerTitle = getIntent().getExtras().getString("marker_title");
            //drawPassedMarkerPath(NMAP_PATHSTART, passedMarkerGeoPoint);
            //Toast.makeText(thisContext, "Path Size : "+pathMarkerList.size(), Toast.LENGTH_SHORT).show();

            //drawPassedMarker(passedMarkerGeoPoint, passedMarkerTitle);

            //TODO : 2016-06-07 현재 위치 시작점
            //현재 위치
            Location curloc = mixContext.getCurrentLocation();
            drawPassedMarkerPath(new NGeoPoint(curloc.getLongitude(), curloc.getLatitude()), passedMarkerGeoPoint);
            drawPath(pathMarkerList);
        }
        //}
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {

        stopMyLocation();

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        // save map view state such as map center position and zoom level.
        saveInstanceState();

        super.onDestroy();
    }


    /* Test Functions */

    private void startMyLocation() {

        if (mMyLocationOverlay != null) {
            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }

            if (mMapLocationManager.isMyLocationEnabled()) {

                if (!mMapView.isAutoRotateEnabled()) {
                    mMyLocationOverlay.setCompassHeadingVisible(true);

                    mMapCompassManager.enableCompass();

                    mMapView.setAutoRotateEnabled(true, false);

                    mMapContainerView.requestLayout();
                } else {
                    stopMyLocation();
                }

                mMapView.postInvalidate();
            } else {
                boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);
                if (!isMyLocationEnabled) {
                    Toast.makeText(thisContext, "Please enable a My Location source in system settings",
                            Toast.LENGTH_LONG).show();

                    Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);

                    return;
                }
            }
        }
    }

    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMapLocationManager.disableMyLocation();

            if (mMapView.isAutoRotateEnabled()) {
                mMyLocationOverlay.setCompassHeadingVisible(false);

                mMapCompassManager.disableCompass();

                mMapView.setAutoRotateEnabled(false, false);

                mMapContainerView.requestLayout();
            }
        }
    }

    private void drawPassedMarker(/*NGeoPoint nGeoPoint, String markerTitle*/ JSONArray markerList) {
        // Markers for POI item
        int markerId = NMapPOIflagType.PIN;

        NMapPOIdata poiData = new NMapPOIdata(markerList.length(), mMapViewerResourceProvider);
        poiData.beginPOIdata(markerList.length());
        try {
            // set POI data
            for(int i=0; i<markerList.length(); i++) {
                poiData.addPOIitem( markerList.getJSONObject(i).getDouble("longitude"), markerList.getJSONObject(i).getDouble("latitude"), markerList.getJSONObject(i).getString("title"), markerId, 0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        poiData.endPOIdata();

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

        // select an item
        //poiDataOverlay.selectPOIitem(0, true);

        // show all POI data
        poiDataOverlay.showAllPOIdata(0);
    }

    //TODO : naver url request for route
    private void drawPassedMarkerPath(NGeoPoint startPoint, NGeoPoint endPoint) {
        final String url = DataSource.createNaverMapRequestURL(startPoint.getLongitude(), startPoint.getLatitude(), endPoint.getLongitude(), endPoint.getLatitude());
        String result = "";

        httpConnection = new HttpConnection();

        try {
            result = (new HttpConnection()).execute(url).get();
            Log.d("NaverJson", result);

            //List<Marker> markers = parseJSONtoMarker(result);
            pathMarkerList =  parseJSONtoMarker(result);
            //drawPath(markers);
            Log.d("NaverGetPath", "get Json data done");
        } catch ( Exception e ) {
            Log.e("HttpConnection", "" + e);
        }
    }

    private List<Marker> parseJSONtoMarker(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        Json jsonClass = new Json();
        List<Marker> markers = jsonClass.load(root, DataSource.DATAFORMAT.NAVER);

        return markers;
    }

    private void drawPath(List<Marker> markers) {

        NMapPOIdata poiData = new NMapPOIdata(markers.size(), mMapViewerResourceProvider, true);
        poiData.beginPOIdata(markers.size());
        //시작점
        poiData.addPOIitem((int)markers.get(0).getLongitude(), (int)markers.get(0).getLatitude(), markers.get(0).getTitle(), NMapPOIflagType.FROM, null);
        //보간점
        for(int i=1; i<markers.size()-1; i++) {
            poiData.addPOIitem((int)markers.get(i).getLongitude(), (int)markers.get(i).getLatitude(), markers.get(i).getDescription(), NMapPOIflagType.NUMBER_BASE + i, null);
        }
        //끝점
        poiData.addPOIitem((int)markers.get(markers.size()-1).getLongitude(), (int)markers.get(markers.size()- 1).getLatitude(), markers.get(markers.size()-1).getTitle(), NMapPOIflagType.TO, null);
        poiData.endPOIdata();

        // create POI data overlay
        final NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

        //TODO : draw path polyline
        // set path data points
        NMapPathData pathData = new NMapPathData(markers.size());

        pathData.initPathData();
        for(int i=0; i<markers.size(); i++) {
            pathData.addPathPoint((int)markers.get(i).getLongitude(), (int)markers.get(i).getLatitude(), NMapPathLineStyle.TYPE_SOLID);
        }
        pathData.endPathData();

        NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

        if (pathDataOverlay != null) {
            // set path line style
            NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
            pathLineStyle.setPataDataType(NMapPathLineStyle.DATA_TYPE_POLYGON);
            pathLineStyle.setLineColor(0xA04DD2, 0xff);
            pathLineStyle.setFillColor(0xFFFFFF, 0x00);

            // show all path data
            pathDataOverlay.showAllPathData(0);
        }
    }

    private void drawMarker(List<Marker> markers) {

        // set path data points
        NMapPathData pathData = new NMapPathData(markers.size());

        pathData.initPathData();
        for(int i=0; i<markers.size(); i++) {
            pathData.addPathPoint(markers.get(i).getLongitude(), markers.get(i).getLatitude(), NMapPathLineStyle.TYPE_SOLID);
        }
        pathData.endPathData();

        NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

        if (pathDataOverlay != null) {
            // set path line style
            NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
            pathLineStyle.setPataDataType(NMapPathLineStyle.DATA_TYPE_POLYGON);
            pathLineStyle.setLineColor(0xA04DD2, 0xff);
            pathLineStyle.setFillColor(0xFFFFFF, 0x00);

            // show all path data
            pathDataOverlay.showAllPathData(0);
        }
    }

    private void testPathDataOverlay() {

        // set path data points
        NMapPathData pathData = new NMapPathData(9);

        pathData.initPathData();
        pathData.addPathPoint(127.108099, 37.366034, NMapPathLineStyle.TYPE_SOLID);
        pathData.addPathPoint(127.108088, 37.366043, 0);
        pathData.addPathPoint(127.108079, 37.365619, 0);
        pathData.addPathPoint(127.107458, 37.365608, 0);
        pathData.addPathPoint(127.107232, 37.365608, 0);
        pathData.addPathPoint(127.106904, 37.365624, 0);
        pathData.addPathPoint(127.105933, 37.365621, NMapPathLineStyle.TYPE_DASH);
        pathData.addPathPoint(127.105929, 37.366378, 0);
        pathData.addPathPoint(127.106279, 37.366380, 0);
        pathData.endPathData();

        NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);
        if (pathDataOverlay != null) {

            // add path data with polygon type
            NMapPathData pathData2 = new NMapPathData(4);
            pathData2.initPathData();
            pathData2.addPathPoint(127.106, 37.367, NMapPathLineStyle.TYPE_SOLID);
            pathData2.addPathPoint(127.107, 37.367, 0);
            pathData2.addPathPoint(127.107, 37.368, 0);
            pathData2.addPathPoint(127.106, 37.368, 0);
            pathData2.endPathData();
            pathDataOverlay.addPathData(pathData2);
            // set path line style
            NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
            pathLineStyle.setPataDataType(NMapPathLineStyle.DATA_TYPE_POLYGON);
            pathLineStyle.setLineColor(0xA04DD2, 0xff);
            pathLineStyle.setFillColor(0xFFFFFF, 0x00);
            pathData2.setPathLineStyle(pathLineStyle);

            // add circle data
            NMapCircleData circleData = new NMapCircleData(1);
            circleData.initCircleData();
            circleData.addCirclePoint(127.1075, 37.3675, 50.0F);
            circleData.endCircleData();
            pathDataOverlay.addCircleData(circleData);
            // set circle style
            NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());
            circleStyle.setLineType(NMapPathLineStyle.TYPE_DASH);
            circleStyle.setFillColor(0x000000, 0x00);
            circleData.setCircleStyle(circleStyle);

            // show all path data
            pathDataOverlay.showAllPathData(0);
        }
    }

    private void testPathPOIdataOverlay() {

        drawPassedMarkerPath(NMAP_PATHSTART, passedMarkerGeoPoint);

        drawPath(pathMarkerList);

        // set POI data
        /*
        NMapPOIdata poiData = new NMapPOIdata(4, mMapViewerResourceProvider, true);
        poiData.beginPOIdata(4);
        poiData.addPOIitem(349652983, 149297368, "Pizza 124-456", NMapPOIflagType.FROM, null);
        poiData.addPOIitem(349652966, 149296906, null, NMapPOIflagType.NUMBER_BASE + 1, null);
        poiData.addPOIitem(349651062, 149296913, null, NMapPOIflagType.NUMBER_BASE + 999, null);
        poiData.addPOIitem(349651376, 149297750, "Pizza 000-999", NMapPOIflagType.TO, null);
        poiData.endPOIdata();
        */

        /*
        //todo : 투래빗 ~ 홈플러스
        NMapPOIdata poiData = new NMapPOIdata(8, mMapViewerResourceProvider, true);
        poiData.beginPOIdata(8);
        poiData.addPOIitem(349624627, 149166532, "투래빗", NMapPOIflagType.FROM, null);
        poiData.addPOIitem(349624263, 149166673, null, NMapPOIflagType.NUMBER_BASE + 1, null);
        poiData.addPOIitem(349623900, 149166671, null, NMapPOIflagType.NUMBER_BASE + 2, null);
        poiData.addPOIitem(349621820, 149168902, null, NMapPOIflagType.NUMBER_BASE + 3, null);
        poiData.addPOIitem(349620113, 149170328, null, NMapPOIflagType.NUMBER_BASE + 4, null);
        poiData.addPOIitem(349620323, 149170680, null, NMapPOIflagType.NUMBER_BASE + 5, null);
        poiData.addPOIitem(349620579, 149171010, null, NMapPOIflagType.NUMBER_BASE + 6, null);
        poiData.addPOIitem(349620903, 149170722, "홈플러스 영통", NMapPOIflagType.TO, null);
        poiData.endPOIdata();

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);
        */

    }

    private void testPOIdataOverlay() {

        // Markers for POI item
        int markerId = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(2, mMapViewerResourceProvider);
        poiData.beginPOIdata(2);
        NMapPOIitem item = poiData.addPOIitem(127.0630205, 37.5091300, "Pizza 777-111", markerId, 0);
        item.setRightAccessory(true, NMapPOIflagType.CLICKABLE_ARROW);
        poiData.addPOIitem(127.061, 37.51, "Pizza 123-456", markerId, 0);
        //TODO : add mixare marker
        if(passedMarkerGeoPoint != null) {
            poiData.addPOIitem(passedMarkerGeoPoint.getLongitude(), passedMarkerGeoPoint.getLatitude(), passedMarkerTitle, markerId, 0);
        }
        poiData.endPOIdata();

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

        // select an item
        poiDataOverlay.selectPOIitem(0, true);

        // show all POI data
        //poiDataOverlay.showAllPOIdata(0);
    }

    private void testFloatingPOIdataOverlay() {
        // Markers for POI item
        int marker1 = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(1, mMapViewerResourceProvider);
        poiData.beginPOIdata(1);
        NMapPOIitem item = poiData.addPOIitem(null, "Touch & Drag to Move", marker1, 0);
        if (item != null) {
            // initialize location to the center of the map view.
            item.setPoint(mMapController.getMapCenter());
            // set floating mode
            item.setFloatingMode(NMapPOIitem.FLOATING_TOUCH | NMapPOIitem.FLOATING_DRAG);
            // show right button on callout
            item.setRightButton(true);

            mFloatingPOIitem = item;
        }
        poiData.endPOIdata();

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);
        if (poiDataOverlay != null) {
            poiDataOverlay.setOnFloatingItemChangeListener(onPOIdataFloatingItemChangeListener);

            // set event listener to the overlay
            poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

            poiDataOverlay.selectPOIitem(0, false);

            mFloatingPOIdataOverlay = poiDataOverlay;
        }
    }

    /* NMapDataProvider Listener */
    private final OnDataProviderListener onDataProviderListener = new OnDataProviderListener() {

        @Override
        public void onReverseGeocoderResponse(NMapPlacemark placeMark, NMapError errInfo) {

            if (DEBUG) {
                Log.i(LOG_TAG, "onReverseGeocoderResponse: placeMark="
                        + ((placeMark != null) ? placeMark.toString() : null));
            }

            if (errInfo != null) {
                Log.e(LOG_TAG, "Failed to findPlacemarkAtLocation: error=" + errInfo.toString());

                Toast.makeText(thisContext, errInfo.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
                mFloatingPOIdataOverlay.deselectFocusedPOIitem();

                if (placeMark != null) {
                    mFloatingPOIitem.setTitle(placeMark.toString());
                }
                mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
            }
        }

    };

    /* MyLocation Listener */
    private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {

        @Override
        public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {

            if (mMapController != null) {
                mMapController.animateTo(myLocation);
            }

            return true;
        }

        @Override
        public void onLocationUpdateTimeout(NMapLocationManager locationManager) {

            // stop location updating
            //			Runnable runnable = new Runnable() {
            //				public void run() {
            //					stopMyLocation();
            //				}
            //			};
            //			runnable.run();

            Toast.makeText(thisContext, "Your current location is temporarily unavailable.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {

            Toast.makeText(thisContext, "Your current location is unavailable area.", Toast.LENGTH_LONG).show();

            stopMyLocation();
        }

    };

    /* MapView State Change Listener*/
    private final NMapView.OnMapStateChangeListener onMapViewStateChangeListener = new NMapView.OnMapStateChangeListener() {

        @Override
        public void onMapInitHandler(NMapView mapView, NMapError errorInfo) {

            if (errorInfo == null) { // success
                // restore map view state such as map center position and zoom level.
                restoreInstanceState();

            } else { // fail
                Log.e(LOG_TAG, "onFailedToInitializeWithError: " + errorInfo.toString());

                Toast.makeText(thisContext, errorInfo.toString(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAnimationStateChange(NMapView mapView, int animType, int animState) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAnimationStateChange: animType=" + animType + ", animState=" + animState);
            }
        }

        @Override
        public void onMapCenterChange(NMapView mapView, NGeoPoint center) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onMapCenterChange: center=" + center.toString());
            }
        }

        @Override
        public void onZoomLevelChange(NMapView mapView, int level) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onZoomLevelChange: level=" + level);
            }
        }

        @Override
        public void onMapCenterChangeFine(NMapView mapView) {

        }
    };

    private final NMapView.OnMapViewTouchEventListener onMapViewTouchEventListener = new NMapView.OnMapViewTouchEventListener() {

        @Override
        public void onLongPress(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onLongPressCanceled(NMapView mapView) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSingleTapUp(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTouchDown(NMapView mapView, MotionEvent ev) {

        }

        @Override
        public void onScroll(NMapView mapView, MotionEvent e1, MotionEvent e2) {
        }

        @Override
        public void onTouchUp(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

    };

    private final NMapView.OnMapViewDelegate onMapViewTouchDelegate = new NMapView.OnMapViewDelegate() {

        @Override
        public boolean isLocationTracking() {
            if (mMapLocationManager != null) {
                if (mMapLocationManager.isMyLocationEnabled()) {
                    return mMapLocationManager.isMyLocationFixed();
                }
            }
            return false;
        }

    };

    /* POI data State Change Listener*/
    private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {

        @Override
        public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
            }

            // [[TEMP]] handle a click event of the callout
            Toast.makeText(thisContext, "onCalloutClick: " + item.getTitle(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                if (item != null) {
                    Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
                } else {
                    Log.i(LOG_TAG, "onFocusChanged: ");
                }
            }
        }
    };

    private final NMapPOIdataOverlay.OnFloatingItemChangeListener onPOIdataFloatingItemChangeListener = new NMapPOIdataOverlay.OnFloatingItemChangeListener() {

        @Override
        public void onPointChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            NGeoPoint point = item.getPoint();

            if (DEBUG) {
                Log.i(LOG_TAG, "onPointChanged: point=" + point.toString());
            }

            findPlacemarkAtLocation(point.longitude, point.latitude);

            item.setTitle(null);

        }
    };

    private final NMapOverlayManager.OnCalloutOverlayListener onCalloutOverlayListener = new NMapOverlayManager.OnCalloutOverlayListener() {

        @Override
        public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem,
                                                         Rect itemBounds) {

            // handle overlapped items
            if (itemOverlay instanceof NMapPOIdataOverlay) {
                NMapPOIdataOverlay poiDataOverlay = (NMapPOIdataOverlay)itemOverlay;

                // check if it is selected by touch event
                if (!poiDataOverlay.isFocusedBySelectItem()) {
                    int countOfOverlappedItems = 1;

                    NMapPOIdata poiData = poiDataOverlay.getPOIdata();
                    for (int i = 0; i < poiData.count(); i++) {
                        NMapPOIitem poiItem = poiData.getPOIitem(i);

                        // skip selected item
                        if (poiItem == overlayItem) {
                            continue;
                        }

                        // check if overlapped or not
                        if (Rect.intersects(poiItem.getBoundsInScreen(), overlayItem.getBoundsInScreen())) {
                            countOfOverlappedItems++;
                        }
                    }

                    if (countOfOverlappedItems > 1) {
                        String text = countOfOverlappedItems + " overlapped items for " + overlayItem.getTitle();
                        Toast.makeText(thisContext, text, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                }
            }

            /*
            // use custom old callout overlay
            if (overlayItem instanceof NMapPOIitem) {
                NMapPOIitem poiItem = (NMapPOIitem)overlayItem;

                if (poiItem.showRightButton()) {
                    return new NMapCalloutCustomOldOverlay(itemOverlay, overlayItem, itemBounds,
                            mMapResourceProvider);
                }
            }
            */

            // use custom callout overlay
            return new NMapCalloutCustomOverlay(itemOverlay, overlayItem, itemBounds, mMapViewerResourceProvider);

            // set basic callout overlay
            //return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
        }

    };

    private final NMapOverlayManager.OnCalloutOverlayViewListener onCalloutOverlayViewListener = new NMapOverlayManager.OnCalloutOverlayViewListener() {

        @Override
        public View onCreateCalloutOverlayView(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {

            /*
            if (overlayItem != null) {
                // [TEST] 말풍선 오버레이를 뷰로 설정함
                String title = overlayItem.getTitle();
                if (title != null && title.length() > 5) {
                    return new NMapCalloutCustomOverlayView(thisContext, itemOverlay, overlayItem, itemBounds);
                }
            }
            */

            // null을 반환하면 말풍선 오버레이를 표시하지 않음
            return null;
        }

    };

    /* Local Functions */
    private static boolean mIsMapEnlared = false;

    private void restoreInstanceState() {
        mPreferences = getPreferences(MODE_PRIVATE);

        int longitudeE6 = mPreferences.getInt(KEY_CENTER_LONGITUDE, NMAP_LOCATION_DEFAULT.getLongitudeE6());
        int latitudeE6 = mPreferences.getInt(KEY_CENTER_LATITUDE, NMAP_LOCATION_DEFAULT.getLatitudeE6());
        int level = mPreferences.getInt(KEY_ZOOM_LEVEL, NMAP_ZOOMLEVEL_DEFAULT);
        int viewMode = mPreferences.getInt(KEY_VIEW_MODE, NMAP_VIEW_MODE_DEFAULT);
        boolean trafficMode = mPreferences.getBoolean(KEY_TRAFFIC_MODE, NMAP_TRAFFIC_MODE_DEFAULT);
        boolean bicycleMode = mPreferences.getBoolean(KEY_BICYCLE_MODE, NMAP_BICYCLE_MODE_DEFAULT);

        mMapController.setMapViewMode(viewMode);
        mMapController.setMapViewTrafficMode(trafficMode);
        mMapController.setMapViewBicycleMode(bicycleMode);
        mMapController.setMapCenter(new NGeoPoint(longitudeE6, latitudeE6), level);

        if (mIsMapEnlared) {
            mMapView.setScalingFactor(2.0F);
        } else {
            mMapView.setScalingFactor(1.0F);
        }
    }

    private void saveInstanceState() {
        if (mPreferences == null) {
            return;
        }

        NGeoPoint center = mMapController.getMapCenter();
        int level = mMapController.getZoomLevel();
        int viewMode = mMapController.getMapViewMode();
        boolean trafficMode = mMapController.getMapViewTrafficMode();
        boolean bicycleMode = mMapController.getMapViewBicycleMode();

        SharedPreferences.Editor edit = mPreferences.edit();

        edit.putInt(KEY_CENTER_LONGITUDE, center.getLongitudeE6());
        edit.putInt(KEY_CENTER_LATITUDE, center.getLatitudeE6());
        edit.putInt(KEY_ZOOM_LEVEL, level);
        edit.putInt(KEY_VIEW_MODE, viewMode);
        edit.putBoolean(KEY_TRAFFIC_MODE, trafficMode);
        edit.putBoolean(KEY_BICYCLE_MODE, bicycleMode);

        edit.commit();

    }

    /* Menus */
    private static final int MENU_ITEM_CLEAR_MAP = 10;
    private static final int MENU_ITEM_MAP_MODE = 20;
    private static final int MENU_ITEM_MAP_MODE_SUB_VECTOR = MENU_ITEM_MAP_MODE + 1;
    private static final int MENU_ITEM_MAP_MODE_SUB_SATELLITE = MENU_ITEM_MAP_MODE + 2;
    private static final int MENU_ITEM_MAP_MODE_SUB_TRAFFIC = MENU_ITEM_MAP_MODE + 3;
    private static final int MENU_ITEM_MAP_MODE_SUB_BICYCLE = MENU_ITEM_MAP_MODE + 4;
    private static final int MENU_ITEM_ZOOM_CONTROLS = 30;
    private static final int MENU_ITEM_MY_LOCATION = 40;

    private static final int MENU_ITEM_TEST_MODE = 50;
    private static final int MENU_ITEM_TEST_POI_DATA = MENU_ITEM_TEST_MODE + 1;
    private static final int MENU_ITEM_TEST_PATH_DATA = MENU_ITEM_TEST_MODE + 2;
    private static final int MENU_ITEM_TEST_FLOATING_DATA = MENU_ITEM_TEST_MODE + 3;
    private static final int MENU_ITEM_TEST_AUTO_ROTATE = MENU_ITEM_TEST_MODE + 4;
    private static final int MENU_ITEM_TEST_SCALING_FACTOR = MENU_ITEM_TEST_MODE + 5;
    private static final int MENU_ITEM_TEST_NEW_ACTIVITY = MENU_ITEM_TEST_MODE + 7;
    private static final int MENU_ITEM_TEST_VISIBLE_BOUNDS = MENU_ITEM_TEST_MODE + 8;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     *
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem menuItem = null;
        SubMenu subMenu = null;

        menuItem = menu.add(Menu.NONE, MENU_ITEM_CLEAR_MAP, Menu.CATEGORY_SECONDARY, "초기화");
        menuItem.setAlphabeticShortcut('c');
        menuItem.setIcon(android.R.drawable.ic_menu_revert);

        subMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_MAP_MODE, Menu.CATEGORY_SECONDARY, "지도보기");
        subMenu.setIcon(android.R.drawable.ic_menu_mapmode);

        menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_VECTOR, Menu.NONE, "일반지도");
        menuItem.setAlphabeticShortcut('m');
        menuItem.setCheckable(true);
        menuItem.setChecked(false);

        menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_SATELLITE, Menu.NONE, "위성지도");
        menuItem.setAlphabeticShortcut('s');
        menuItem.setCheckable(true);
        menuItem.setChecked(false);

        menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_TRAFFIC, Menu.NONE, "실시간교통");
        menuItem.setAlphabeticShortcut('t');
        menuItem.setCheckable(true);
        menuItem.setChecked(false);

        menuItem = subMenu.add(0, MENU_ITEM_MAP_MODE_SUB_BICYCLE, Menu.NONE, "자전거지도");
        menuItem.setAlphabeticShortcut('b');
        menuItem.setCheckable(true);
        menuItem.setChecked(false);

        menuItem = menu.add(0, MENU_ITEM_ZOOM_CONTROLS, Menu.CATEGORY_SECONDARY, "Zoom Controls");
        menuItem.setAlphabeticShortcut('z');
        menuItem.setIcon(android.R.drawable.ic_menu_zoom);

        menuItem = menu.add(0, MENU_ITEM_MY_LOCATION, Menu.CATEGORY_SECONDARY, "내위치");
        menuItem.setAlphabeticShortcut('l');
        menuItem.setIcon(android.R.drawable.ic_menu_mylocation);

        subMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_TEST_MODE, Menu.CATEGORY_SECONDARY, "테스트");
        subMenu.setIcon(android.R.drawable.ic_menu_more);

        menuItem = subMenu.add(0, MENU_ITEM_TEST_NEW_ACTIVITY, Menu.NONE, "New Activity");
        menuItem.setAlphabeticShortcut('n');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_VISIBLE_BOUNDS, Menu.NONE, "Test Visible Bounds");
        menuItem.setAlphabeticShortcut('v');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_POI_DATA, Menu.NONE, "마커 표시");
        menuItem.setAlphabeticShortcut('p');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_PATH_DATA, Menu.NONE, "경로선 표시");
        menuItem.setAlphabeticShortcut('t');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_FLOATING_DATA, Menu.NONE, "직접 지정");
        menuItem.setAlphabeticShortcut('f');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_SCALING_FACTOR, Menu.NONE, "지도 크게보기");
        menuItem.setAlphabeticShortcut('s');

        menuItem = subMenu.add(0, MENU_ITEM_TEST_AUTO_ROTATE, Menu.NONE, "지도 회전");
        menuItem.setAlphabeticShortcut('a');

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu pMenu) {
        super.onPrepareOptionsMenu(pMenu);

        int viewMode = mMapController.getMapViewMode();
        boolean isTraffic = mMapController.getMapViewTrafficMode();
        boolean isBicycle = mMapController.getMapViewBicycleMode();

        pMenu.findItem(MENU_ITEM_CLEAR_MAP).setEnabled(
                (viewMode != NMapView.VIEW_MODE_VECTOR) || isTraffic || mOverlayManager.sizeofOverlays() > 0);
        pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_VECTOR).setChecked(viewMode == NMapView.VIEW_MODE_VECTOR);
        pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_SATELLITE).setChecked(viewMode == NMapView.VIEW_MODE_HYBRID);
        pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_TRAFFIC).setChecked(isTraffic);
        pMenu.findItem(MENU_ITEM_MAP_MODE_SUB_BICYCLE).setChecked(isBicycle);

        if (mMyLocationOverlay == null) {
            pMenu.findItem(MENU_ITEM_MY_LOCATION).setEnabled(false);
        }

        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case MENU_ITEM_CLEAR_MAP:
                if (mMyLocationOverlay != null) {
                    stopMyLocation();
                    mOverlayManager.removeOverlay(mMyLocationOverlay);
                }

                mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
                mMapController.setMapViewTrafficMode(false);
                mMapController.setMapViewBicycleMode(false);

                mOverlayManager.clearOverlays();

                return true;

            case MENU_ITEM_MAP_MODE_SUB_VECTOR:
                mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
                return true;

            case MENU_ITEM_MAP_MODE_SUB_SATELLITE:
                mMapController.setMapViewMode(NMapView.VIEW_MODE_HYBRID);
                return true;

            case MENU_ITEM_MAP_MODE_SUB_TRAFFIC:
                mMapController.setMapViewTrafficMode(!mMapController.getMapViewTrafficMode());
                return true;

            case MENU_ITEM_MAP_MODE_SUB_BICYCLE:
                mMapController.setMapViewBicycleMode(!mMapController.getMapViewBicycleMode());
                return true;

            case MENU_ITEM_ZOOM_CONTROLS:
                mMapView.displayZoomControls(true);
                return true;

            case MENU_ITEM_MY_LOCATION:
                startMyLocation();
                return true;

            case MENU_ITEM_TEST_POI_DATA:
                mOverlayManager.clearOverlays();

                // add POI data overlay
                testPOIdataOverlay();
                return true;

            case MENU_ITEM_TEST_PATH_DATA:
                mOverlayManager.clearOverlays();

                // add path data overlay
                //testPathDataOverlay();

                // add path POI data overlay
                testPathPOIdataOverlay();
                //drawPassedMarkerPath(new NGeoPoint(NMAP_PATHSTART, passedMarkerGeoPoint);
                return true;

            case MENU_ITEM_TEST_FLOATING_DATA:
                mOverlayManager.clearOverlays();
                testFloatingPOIdataOverlay();
                return true;

            case MENU_ITEM_TEST_NEW_ACTIVITY:
                //Intent intent = new Intent(this, FragmentActivity.class);
                //startActivity(intent);
                return true;

            case MENU_ITEM_TEST_VISIBLE_BOUNDS:
                // test visible bounds
                Rect viewFrame = mMapView.getMapController().getViewFrameVisible();
                mMapController.setBoundsVisible(0, 0, viewFrame.width(), viewFrame.height() - 200);

                // add POI data overlay
                mOverlayManager.clearOverlays();

                testPathDataOverlay();
                return true;

            case MENU_ITEM_TEST_SCALING_FACTOR:
                if (mMapView.getMapProjection().isProjectionScaled()) {
                    if (mMapView.getMapProjection().isMapHD()) {
                        mMapView.setScalingFactor(2.0F, false);
                    } else {
                        mMapView.setScalingFactor(1.0F, false);
                    }
                } else {
                    mMapView.setScalingFactor(2.0F, true);
                }
                mIsMapEnlared = mMapView.getMapProjection().isProjectionScaled();
                return true;

            case MENU_ITEM_TEST_AUTO_ROTATE:
                if (mMapView.isAutoRotateEnabled()) {
                    mMapView.setAutoRotateEnabled(false, false);

                    mMapContainerView.requestLayout();

                    mHnadler.removeCallbacks(mTestAutoRotation);
                } else {

                    mMapView.setAutoRotateEnabled(true, false);

                    mMapView.setRotateAngle(30);
                    mHnadler.postDelayed(mTestAutoRotation, AUTO_ROTATE_INTERVAL);

                    mMapContainerView.requestLayout();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final long AUTO_ROTATE_INTERVAL = 2000;
    private final Handler mHnadler = new Handler();
    private final Runnable mTestAutoRotation = new Runnable() {
        @Override
        public void run() {
//        	if (mMapView.isAutoRotateEnabled()) {
//    			float degree = (float)Math.random()*360;
//
//    			degree = mMapView.getRoateAngle() + 30;
//
//    			mMapView.setRotateAngle(degree);
//
//            	mHnadler.postDelayed(mTestAutoRotation, AUTO_ROTATE_INTERVAL);
//        	}
        }
    };

    /**
     * Container view class to rotate map view.
     */
    private class MapContainerView extends ViewGroup {

        public MapContainerView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int width = getWidth();
            final int height = getHeight();
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                final int childWidth = view.getMeasuredWidth();
                final int childHeight = view.getMeasuredHeight();
                final int childLeft = (width - childWidth) / 2;
                final int childTop = (height - childHeight) / 2;
                view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }

            if (changed) {
                mOverlayManager.onSizeChanged(width, height);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            int sizeSpecWidth = widthMeasureSpec;
            int sizeSpecHeight = heightMeasureSpec;

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);

                if (view instanceof NMapView) {
                    if (mMapView.isAutoRotateEnabled()) {
                        int diag = (((int)(Math.sqrt(w * w + h * h)) + 1) / 2 * 2);
                        sizeSpecWidth = MeasureSpec.makeMeasureSpec(diag, MeasureSpec.EXACTLY);
                        sizeSpecHeight = sizeSpecWidth;
                    }
                }

                view.measure(sizeSpecWidth, sizeSpecHeight);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {

        }
    }
}
