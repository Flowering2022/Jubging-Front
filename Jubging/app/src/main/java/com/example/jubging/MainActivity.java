package com.example.jubging;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import net.daum.mf.map.api.MapPOIItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    private static final String LOG_TAG = "MainActivity";
    private static final int PRIORITY_HIGH_ACCURACY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private MapView mapView;
    private ViewGroup mapViewContainer;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    MapPOIItem[] marker;
    Double current_latitude;
    Double current_longitude;
    JSONArray jsonArray;
    private PermissionSupport permission;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback mLocationCallback;
    private  double mLatitude;
    private double mLongitude;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        permissionCheck();

        //지도를 띄우자
        // java code
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.user, new MapPOIItem.ImageOffset(16, 16));

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }

        ImageButton btn = (ImageButton) findViewById(R.id.trc);
        btn.bringToFront();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
            }
        });

        ImageButton button = (ImageButton) findViewById(R.id.location_Btn);
        button.bringToFront();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    mapView.removeAllPOIItems();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        //위도 추출
                        Double latitude = Double.parseDouble(obj.getString("latitude"));

                        //경도 추출
                        Double longitude = Double.parseDouble(obj.getString("longitude"));

                        //클린하우스 이름 추출
                        String location = obj.getString("location");

                        double distanceKiloMeter =
                                distance(latitude, longitude, current_latitude, current_longitude, "kilometer");


                        // 현재 위치에서 부터 1.5km거리 안에 있는 마커만 표시하기
                        if (distanceKiloMeter < 1.5) {
                            MapPoint tempmapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                            marker[i] = new MapPOIItem();
                            marker[i].setTag(i + 1);
                            marker[i].setItemName(location);
                            marker[i].setMapPoint(tempmapPoint);
                            marker[i].setMarkerType(MapPOIItem.MarkerType.CustomImage); // 기본으로 제공하는 BluePin 마커 모양.
                            marker[i].setCustomImageResourceId(R.drawable.markernc);
                            marker[i].setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);// 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
                            marker[i].setCustomSelectedImageResourceId(R.drawable.markertk);
                            mapView.addPOIItem(marker[i]);
                        }


                    }
                } catch (JSONException e) {

                }
            }
        });


        //Json 파싱
        StringBuilder urlBuilder = new StringBuilder("https://gist.githubusercontent.com/Yummy-sk/162dd1e4349ebf821f43db6c3c67f744/raw/ed25686c4f36e2b1474a8eeab2fa52837bdb5d93/jeju_clean_house"); /*URL*/


        // 3. URL 객체 생성.
        new Thread(() -> {
            URL url = null;
            HttpURLConnection conn = null;
            BufferedReader rd = null;
            try {
                url = new URL(urlBuilder.toString());
                // 4. 요청하고자 하는 URL과 통신하기 위한 Connection 객체 생성.
                conn = (HttpURLConnection) url.openConnection();
                // 5. 통신을 위한 메소드 SET.
                conn.setRequestMethod("GET");
                // 6. 통신을 위한 Content-type SET.
                conn.setRequestProperty("Content-type", "application/json");
                // 7. 통신 응답 코드 확인.
                System.out.println("Response code: " + conn.getResponseCode());
                // 8. 전달받은 데이터를 BufferedReader 객체로 저장.

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                // 9. 저장된 데이터를 라인별로 읽어 StringBuilder 객체로 저장.
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }

                // 11. 전달받은 데이터 확인.
//                System.out.println(sb.toString());

                String data = sb.toString();
//                System.out.println(data);
                jsonArray = new JSONArray(data);

                marker = new MapPOIItem[jsonArray.length()];
                // jsonArray.length()


//                mapView.addPOIItems(marker);
                mapView.setPOIItemEventListener(this);


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                // 10. 객체 해제.

                if (rd != null) {
                    try {
                        rd.close();
                    } catch (IOException e) {

                    }
                }

                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();


        MapPolyline polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 255, 51, 0)); // Polyline 컬러 지정.
        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, cts.getToken()).addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Log.d("구글지도", "onSuccess위도: " + location.getLatitude());
                    Log.d("구글지도", "onSuccess경도: " + location.getLongitude());
                    polyline.addPoint(MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude()));
                }
            }
        });
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(1000*100);
        locationRequest.setInterval(1000*100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                mLatitude=location.getLatitude();
                mLongitude=location.getLongitude();
                Log.d("구글", "onLocationResult 위도: "+mLatitude);
                Log.d("구글", "onLocationResult 경도: "+mLongitude);
                // Log.d("구글", "onLocationResult: "+location.);
                // Polyline 좌표 지정.
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(mLatitude, mLongitude));
                mapView.addPolyline(polyline);
            }
        };


        //fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());

    }


    private void permissionCheck() {

        // PermissionSupport.java 클래스 객체 생성
        permission = new PermissionSupport(this, this);

        // 권한 체크 후 리턴이 false로 들어오면
        if (!permission.checkPermission()) {
            //권한 요청
            permission.requestPermission();
        }
    }

    // Request Permission에 대한 결과 값 받아와
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //여기서도 리턴이 false로 들어온다면 (사용자가 권한 허용 거부)
        if (!permission.permissionResult(requestCode, permissions, grantResults)) {
            // 다시 permission 요청
            permission.requestPermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    // 거리 구하기 (점과 점 사이의 거리)
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit == "kilometer") {
            dist = dist * 1.609344;
        } else if (unit == "meter") {
            dist = dist * 1609.344;
        }

        return (dist);
    }


    // This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    // This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    @Override
    protected void onResume() {
        super.onResume();

        startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(5000);
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient.requestLocationUpdates(locationRequest,
                mLocationCallback,
                Looper.getMainLooper());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapViewContainer.removeAllViews();
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {
    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {
    }


    private void onFinishReverseGeoCoding(String result) {
//        Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }


    void checkRunTimePermission() {

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음

        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {
        Log.d("TAG", "onMapViewInitialized: " + mapView.getPOIItems().length);
    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        Log.d("TAG", "onMapViewSingleTapped: " + mapView.getPOIItems().length);
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }


    //지도를 드래그 했을 때 이벤트
    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        Log.d("TAG", "onMapViewDragStarted: ");
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);

        current_latitude = mapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
        current_longitude = mapView.getMapCenterPoint().getMapPointGeoCoord().longitude;

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    MapPOIItem temp = null;

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        if (mapView != null && mapPOIItem != null) {
            Log.d("TAG", "onPOIItemSelected: " + mapPOIItem.getItemName() + " " + mapView.isSelected());
        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        // 말풍선 클릭
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    public class PermissionSupport {

        private Context context;
        private Activity activity;

        //요청할 권한 배열 저장
        private String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION

        };
        private List permissionList;

        //권한 요청시 발생하는 창에 대한 결과값을 받기 위해 지정해주는 int 형
        //원하는 임의의 숫자 지정
        private final int MULTIPLE_PERMISSIONS = 1023; //요청에 대한 결과값 확인을 위해 RequestCode를 final로 정의

        //생성자에서 Activity와 Context를 파라미터로 받아
        public PermissionSupport(Activity _activity, Context _context){
            this.activity = _activity;
            this.context = _context;
        }

        //배열로 선언한 권한 중 허용되지 않은 권한 있는지 체크
        public boolean checkPermission() {
            int result;
            permissionList = new ArrayList<>();

            for(String pm : permissions){
                result = ContextCompat.checkSelfPermission(context, pm);
                if(result != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(pm);
                }
            }
            if(!permissionList.isEmpty()){
                return false;
            }
            return true;
        }

        //배열로 선언한 권한에 대해 사용자에게 허용 요청
        public void requestPermission(){
            ActivityCompat.requestPermissions(activity, (String[]) permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
        }

        //요청한 권한에 대한 결과값 판단 및 처리
        public boolean permissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
            //우선 requestCode가 아까 위에 final로 선언하였던 숫자와 맞는지, 결과값의 길이가 0보다는 큰지 먼저 체크
            if(requestCode == MULTIPLE_PERMISSIONS && (grantResults.length >0)) {
                for(int i=0; i< grantResults.length; i++){
                    //grantResults 가 0이면 사용자가 허용한 것 / -1이면 거부한 것
                    //-1이 있는지 체크하여 하나라도 -1이 나온다면 false를 리턴
                    if(grantResults[i] == -1){
                        return false;
                    }
                }
            }
            return true;
        }

    }
}

