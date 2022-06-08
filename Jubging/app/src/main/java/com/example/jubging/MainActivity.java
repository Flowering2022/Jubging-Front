package com.example.jubging;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import net.daum.mf.map.api.CameraUpdateFactory;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener
{
    private static final String LOG_TAG = "MainActivity";
    private static final int PRIORITY_HIGH_ACCURACY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private MapView mapView;
    private ViewGroup mapViewContainer;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    MapPOIItem[] marker;
    MapPOIItem trackingMarker;
    Double current_latitude;
    Double current_longitude;
    JSONArray jsonArray;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback mLocationCallback;
    private double mLatitude;
    private double mLongitude;
    private  double totalDistance=0;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        //지도를 띄우자
        // java code
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.blank, new MapPOIItem.ImageOffset(16, 16));

        trackingMarker= new MapPOIItem();
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }

        final ImageButton btn = (ImageButton) findViewById(R.id.trc);
        btn.bringToFront();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MapPointBounds mapPointBounds = new MapPointBounds();
                MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(mLatitude, mLongitude);
                mapPointBounds.add(mapPoint);
                mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds));


            }
        });

        //서버 연결헤서 데이터 가져오기
        //Retrofit 인스턴스 생성
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("http://ec2-52-79-240-128.ap-northeast-2.compute.amazonaws.com/") //baseUrl
                .addConverterFactory(GsonConverterFactory.create()) //JSON을 변환해줄 Gson 변환기 등록
                .build();

        RetrofitInterface service = retrofit.create(RetrofitInterface.class); //RetrofitInterface 겍체 구현

        final Button btn_finish = findViewById(R.id.btn_finish); //전송 버튼

        final ImageButton btn_pause = findViewById(R.id.btn_pause);
        btn_pause.bringToFront();

        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_pause.setVisibility(View.INVISIBLE); //시작버튼 누르면 시작버튼 없애기
                btn_finish.setVisibility(View.VISIBLE); //운동완료 버튼 보이기
                btn_finish.setEnabled(true); //운동완료버튼 활성화
                btn_finish.setText("플로깅 완료하기"); //버튼 내 택스트 변경
            }
        });

        btn_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FinishActivity.class);
                startActivity(intent);
            }
        });
//        //Post.. url을 못가져와서 인가..
//        Call<List<JsonPlaceHOlderApi.Post>> call = jsonPlaceHOlderApi.getPosts();
//
//        call.enqueue(new Callback<List<JsonPlaceHOlderApi.Post>>() {
//            @Override
//            public void onResponse(Call<List<JsonPlaceHOlderApi.Post>> call, Response<List<JsonPlaceHOlderApi.Post>> response) {
//                if (!response.isSuccessful())
//                {
//                    distance.setText("Code: " + response.code());
//                    return;
//                }
//
//                List<JsonPlaceHOlderApi.Post> posts = response.body();
//
//                for (JsonPlaceHOlderApi.Post post : posts) {
//                    String content ="";
//                    content += "Distance : " + post.getDistance__sum() + "\n";
//
//                    distance.append(content);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<JsonPlaceHOlderApi.Post>> call, Throwable t) {
//                distance.setText(t.getMessage());
//            }
//        });
////////////////////////////////////////////////////////////////////////////////
//        //화면 전환 버튼 동작
//        //Button btn_finish = (Button) findViewById(R.id.btn_finish);
//        btn_finish = findViewById(R.id.btn_finish);
//        btn_finish.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view){
//                Intent intent = new Intent(getApplicationContext(), CompleteActivity.class);
//                startActivity(intent);
//            }
//        });
/////////////////////////////////////////////////////////////////////////////////
//        //플로깅 총 횟수
//        number = findViewById(R.id.number);
//        number.setText(count+"");
//        btn_finish = findViewById(R.id.btn_finish);
//        btn_finish.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                while (count >= 0) {
////                    count++;
////                    btn_count.setText(count + "");
////                }
//                @Override
//                public void onClick(View v) {
//                    if (count == 0){
//                        count++;
//                    }
//                    else {
//                        number.setText(count + "");
//                        count++;
//                    }
//
////                if (count %2 == 1) {
////                    btn_count.setText(count + "");//1 //3
////                    count++;//2 //4
////                }
//
////               if (count ==0){
////                    count++; //1
////                } else if (count == 1) {
////                    btn_count.setText(count + ""); //1
////                    count++; //2
////                }
////                else if(count==2){
////                    count++; //3
////                }
////                else if (count == 3) {
////                    count-=1; //2
////                    btn_count.setText(count + ""); //2
////                    count += 2; //4
////                }
////                else if (count ==4){
////                    count++; //5
////                }
////                else if (count ==5){
////                   count-=2; //3
////                   btn_count.setText(count + ""); //3
////                   count += 3; //6
////               }
////               else if (count ==6){
////                   count++; //7
////               }
////               else if (count ==7){
////                   count-=3; //4
////                   btn_count.setText(count + ""); //4
////                   count += 4; //8
////               }
//
//            }
//        });


        final ImageButton button = (ImageButton) findViewById(R.id.location_Btn);
        button.bringToFront();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    mapView.removeAllPOIItems();
                    trackingMarker.setCustomImageResourceId(R.drawable.userimg); // 마커 이미지.
                    mapView.addPOIItem(trackingMarker);

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
                        if(distanceKiloMeter < 1.5) {
                            MapPoint tempmapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                            marker[i] = new MapPOIItem();
                            marker[i].setTag(100);
                            marker[i].setItemName(location);
                            marker[i].setMapPoint(tempmapPoint);
                            marker[i].setMarkerType(MapPOIItem.MarkerType.CustomImage); // 기본으로 제공하는 BluePin 마커 모양.
                            marker[i].setCustomImageResourceId(R.drawable.markernc);
                            marker[i].setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);// 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
                            marker[i].setCustomSelectedImageResourceId(R.drawable.markertk);
                            mapView.addPOIItem(marker[i]);
                        }


                    }
                }
                catch (JSONException e){

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
        polyline.setLineColor(Color.argb(255, 255, 51, 0)); // Polyline 컬러 지정.


        // Polyline 지도에 올리기.
        mapView.addPolyline(polyline);

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                        }
                    }
                });
        fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, cts.getToken()).addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    Log.d("구글지도", "onSuccess위도: " + location.getLatitude());
                    Log.d("구글지도", "onSuccess경도: " + location.getLongitude());
                    //polyline.addPoint(MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude()));
                }
            }
        });
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(500*60);
        locationRequest.setInterval(500*60);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if(mLatitude != 0 && mLongitude != 0){
                    totalDistance+= distance(mLatitude,mLongitude,location.getLatitude(),location.getLongitude(),"meter");
                }
                mLatitude=location.getLatitude();
                mLongitude=location.getLongitude();
                Log.d("구글", "onLocationResult 위도: "+mLatitude);
                Log.d("구글", "onLocationResult 경도: "+mLongitude);
                Log.d("구글", "onLocationResult 거리: "+totalDistance);
                // Log.d("구글", "onLocationResult: "+location.);
                mapView.removePOIItem(trackingMarker);
                trackingMarker.setItemName("trackinMarker");
                trackingMarker.setTag(0);
                trackingMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(mLatitude,mLongitude));
                trackingMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);// 마커타입을 커스텀 마커로 지정.
                trackingMarker.setCustomImageResourceId(R.drawable.userimg); // 마커 이미지.
                mapView.addPOIItem(trackingMarker);
                // Polyline 좌표 지정.
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(mLatitude, mLongitude));
                mapView.addPolyline(polyline);
            }
        };
        //fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());


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
        } else if(unit == "meter"){
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
        locationRequest.setFastestInterval(10000);
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
    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
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

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);

        mapView.setShowCurrentLocationMarker(false);

        current_latitude = mapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
        current_longitude = mapView.getMapCenterPoint().getMapPointGeoCoord().longitude;

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }


    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
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



}

