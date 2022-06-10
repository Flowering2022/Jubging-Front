package com.example.jubging;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;


public class LogoActivity extends AppCompatActivity {
    private LogoActivity.PermissionSupport permission;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);



        permissionCheck();


        Handler handler = new Handler();
        handler.postDelayed(new splashHandler() ,2000);


    }

    private class splashHandler implements Runnable {
        @Override
        public void run() {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            LogoActivity.this.finish();
        }
    }


//    private void moveMain(int sec) {
//        new Handler().postDelayed(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                //new Intent(현재 context, 이동할 activity)
//                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//
//                startActivity(intent);	//intent 에 명시된 액티비티로 이동
//
//                finish();	//현재 액티비티 종료
//            }
//        }, 1000 * sec); // sec초 정도 딜레이를 준 후 시작
//    }


    private void permissionCheck() {

        // PermissionSupport.java 클래스 객체 생성
        permission = new PermissionSupport(this, this);

        // 권한 체크 후 리턴이 false로 들어오면
        if (!permission.checkPermission()){
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