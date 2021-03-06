package com.example.jubging;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener
{
    public static Context context_main;

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
    MapPolyline polyline;
    private int startTime=0;
    private int recentTime=0;
    private boolean ploggingStart=false;

    private TextView time, plodistance, plospeed;
    private Thread timeThread = null;

    private Boolean isRunning = true;
    String timestr = "";
    int intentdinstance;
    String intentspeed = "";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context_main = this;

        //????????? ?????????
        // java code
        mapView = new MapView(this);
        mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.blank, new MapPOIItem.ImageOffset(16, 16));

        // ?????? TextView
        time = (TextView)findViewById(R.id.runtime);
        // ?????? TextView
        plodistance = (TextView)findViewById(R.id.km);
        // ?????? TextView
        plospeed= (TextView)findViewById(R.id.fast);


        @SuppressLint("HandlerLeak")
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int mSec = msg.arg1 % 100;
                int sec = (msg.arg1 / 100) % 60;
                int min = (msg.arg1 / 100) / 60;
                int hour = (msg.arg1 / 100) / 360;
                //1000??? 1??? 1000*60 ??? 1??? 1000*60*10??? 10??? 1000*60*60??? ?????????

                @SuppressLint("DefaultLocale") String result = String.format("%02d:%02d", min, sec);
                if (result.equals("00:01:15:00")) {
                    Toast.makeText(MainActivity.this, "1??? 15?????? ???????????????.", Toast.LENGTH_SHORT).show();
                }
                time.setText(result);
                timestr = result;
            }
        };

        class timeThread implements Runnable {
            @Override
            public void run() {
                int i = 0;

                while (true) {
                    while (isRunning) { //??????????????? ????????? ??????
                        Message msg = new Message();
                        msg.arg1 = i++;
                        handler.sendMessage(msg);

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    time.setText("");
                                    time.setText("00:00");
                                }
                            });
                            return; // ???????????? ?????? ?????? return
                        }
                    }
                }
            }
        }

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

        //?????? ???????????? ????????? ????????????
        //Retrofit ???????????? ??????
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("http://ec2-52-79-240-128.ap-northeast-2.compute.amazonaws.com/") //baseUrl
                .addConverterFactory(GsonConverterFactory.create()) //JSON??? ???????????? Gson ????????? ??????
                .build();

        RetrofitInterface service = retrofit.create(RetrofitInterface.class); //RetrofitInterface ?????? ??????

        final Button btn_finish = findViewById(R.id.btn_finish); //?????? ??????

        //?????? ??????
        final ImageButton btn_pause = findViewById(R.id.btn_pause);
        btn_pause.bringToFront();
        final TextView time = (TextView)findViewById(R.id.runtime);
        final TextView distan = (TextView)findViewById(R.id.km);


        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_pause.setVisibility(View.INVISIBLE); //???????????? ????????? ???????????? ?????????
                btn_finish.setVisibility(View.VISIBLE); //???????????? ?????? ?????????
                btn_finish.setEnabled(true); //?????????????????? ?????????
                btn_finish.setText("????????? ????????????"); //?????? ??? ????????? ??????

                if(!ploggingStart){
                    ploggingStart=true;
                    startTime= (int) System.currentTimeMillis();

                }


                timeThread = new Thread(new timeThread());
                timeThread.start();
            }
        });




        btn_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isRunning = true;
                mapViewContainer.removeAllViews();

//
                //Post??????
                DataClass_Post post = new DataClass_Post(1, intentdinstance, timestr);
                Call<DataClass_Post> call2 = service.postName(post);

                call2.enqueue(new Callback<DataClass_Post>(){
                    @Override
                    public void onResponse(Call<DataClass_Post> call2, Response<DataClass_Post> response){
                        if(response.isSuccessful()){
                            DataClass_Post result = response.body();
                        }
                        if (!response.isSuccessful()) {
                            return;

                        }

                    }
                    @Override
                    public void onFailure(Call<DataClass_Post> call2, Throwable t){

                    }
                });

                Intent intent = new Intent(getApplicationContext(), FinishActivity.class);
                startActivity(intent);

                if(ploggingStart){
                    ploggingStart=false;
                    mapView.removeAllPolylines();
                    mapView.removePOIItem(trackingMarker);
                }

            }
        });

        final ImageButton button = (ImageButton) findViewById(R.id.location_Btn);
        button.bringToFront();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mapView.removeAllPOIItems();
                    trackingMarker.setCustomImageResourceId(R.drawable.userimg); // ?????? ?????????.
                    mapView.addPOIItem(trackingMarker);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        //?????? ??????
                        Double latitude = Double.parseDouble(obj.getString("latitude"));

                        //?????? ??????
                        Double longitude = Double.parseDouble(obj.getString("longitude"));

                        //??????????????? ?????? ??????
                        String location = obj.getString("location");

                        double distanceKiloMeter =
                                distance(latitude, longitude, current_latitude, current_longitude, "kilometer");


                        // ?????? ???????????? ?????? 1.5km?????? ?????? ?????? ????????? ????????????
                        if(distanceKiloMeter < 4) {
                            MapPoint tempmapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                            marker[i] = new MapPOIItem();
                            marker[i].setTag(100);
                            marker[i].setItemName(location);
                            marker[i].setMapPoint(tempmapPoint);
                            marker[i].setMarkerType(MapPOIItem.MarkerType.CustomImage); // ???????????? ???????????? BluePin ?????? ??????.
                            marker[i].setCustomImageResourceId(R.drawable.markernc);
                            marker[i].setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);// ????????? ???????????????, ???????????? ???????????? RedPin ?????? ??????.
                            marker[i].setCustomSelectedImageResourceId(R.drawable.markertk);
                            mapView.addPOIItem(marker[i]);
                        }


                    }
                }
                catch (JSONException e){

                }
            }
        });




        //Json ??????
        StringBuilder urlBuilder = new StringBuilder("https://gist.githubusercontent.com/Yummy-sk/162dd1e4349ebf821f43db6c3c67f744/raw/ed25686c4f36e2b1474a8eeab2fa52837bdb5d93/jeju_clean_house"); /*URL*/


        // 3. URL ?????? ??????.
        new Thread(() -> {
            URL url = null;
            HttpURLConnection conn = null;
            BufferedReader rd = null;
            try {
                url = new URL(urlBuilder.toString());
                // 4. ??????????????? ?????? URL??? ???????????? ?????? Connection ?????? ??????.
                conn = (HttpURLConnection) url.openConnection();
                // 5. ????????? ?????? ????????? SET.
                conn.setRequestMethod("GET");
                // 6. ????????? ?????? Content-type SET.
                conn.setRequestProperty("Content-type", "application/json");
                // 7. ?????? ?????? ?????? ??????.
                System.out.println("Response code: " + conn.getResponseCode());
                // 8. ???????????? ???????????? BufferedReader ????????? ??????.

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                // 9. ????????? ???????????? ???????????? ?????? StringBuilder ????????? ??????.
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }

                // 11. ???????????? ????????? ??????.
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
                // 10. ?????? ??????.

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

        polyline = new MapPolyline();
        polyline.setTag(1000);
        polyline.setLineColor(Color.argb(255, 255, 51, 0)); // Polyline ?????? ??????.

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
                    Log.d("????????????", "onSuccess??????: " + location.getLatitude());
                    Log.d("????????????", "onSuccess??????: " + location.getLongitude());
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
                if(ploggingStart){
                    recentTime= (int) System.currentTimeMillis();
                    polyline.addPoint(MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude()));
                    mapView.addPolyline(polyline);
                    if(mLatitude != 0 && mLongitude != 0){
                        totalDistance+= distance(mLatitude,mLongitude,location.getLatitude(),location.getLongitude(),"meter");
                    }
                }


                mLatitude=location.getLatitude();
                mLongitude=location.getLongitude();
                Log.d("??????", "onLocationResult ??????: "+mLatitude);
                Log.d("??????", "onLocationResult ??????: "+mLongitude);
                Log.d("??????", "onLocationResult ??????: "+totalDistance);
                Log.d("??????", "onLocationResult ?????? ??????: "+new Date(startTime));
                Log.d("??????", "onLocationResult ?????? ??????: "+new Date(recentTime));
                Log.d("??????", "onLocationResult ??????: "+(recentTime-startTime)/1000/3600+"h"+(recentTime-startTime)/1000/60%60+"m"+(recentTime-startTime)/1000%60+"s");
                Log.d("??????", "onLocationResult ??????: "+totalDistance/((recentTime-startTime)/1000)+"m/h");

                double tempspeed = (totalDistance)/(((recentTime) -startTime)/1000);

                if(Double.isNaN(tempspeed)){
                    tempspeed = 0.001;
                }

                String speed = String.format("%.1f", tempspeed);

                Log.d("??????", "tempspeed: "+tempspeed);


                intentdinstance = (int)totalDistance;
                intentspeed = speed;

                plodistance.setText(((int)totalDistance)+"m");
                plospeed.setText(speed+"m/h");

                // Log.d("??????", "onLocationResult: "+location.);
                mapView.removePOIItem(trackingMarker);
                trackingMarker.setItemName("?????? ??????");
                trackingMarker.setTag(0);
                trackingMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(mLatitude,mLongitude));
                trackingMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);// ??????????????? ????????? ????????? ??????.
                trackingMarker.setCustomImageResourceId(R.drawable.userimg); // ?????? ?????????.
                mapView.addPOIItem(trackingMarker);

            }
        };

        //fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());



    }




    // ?????? ????????? (?????? ??? ????????? ??????)
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit.equals("kilometer")) {
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

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)
            // 3.  ?????? ?????? ????????? ??? ??????

        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.
            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(MainActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ?????????????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
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
                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
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


    //????????? ????????? ?????? ??? ?????????
    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {



    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);

        mapView.setShowCurrentLocationMarker(false);

        current_latitude = mapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
        current_longitude = mapView.getMapCenterPoint().getMapPointGeoCoord().longitude;
    }


    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        // ????????? ??????
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }



}


