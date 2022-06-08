package com.example.jubging;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class FinishActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);


        //서버 연결헤서 데이터 가져오기
        //Retrofit 인스턴스 생성
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("http://ec2-52-79-240-128.ap-northeast-2.compute.amazonaws.com/") //baseUrl
                .addConverterFactory(GsonConverterFactory.create()) //JSON을 변환해줄 Gson 변환기 등록
                .build();

        RetrofitInterface service = retrofit.create(RetrofitInterface.class); //RetrofitInterface 겍체 구현

        final Button btn_finish = findViewById(R.id.btn_finish); //전송 버튼


        final ImageButton btn_pause = findViewById(R.id.restart);

        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });

        //서버 주소/1 전달
        Call<DataClass> call = service.getName("1");

        call.enqueue(new Callback<DataClass>() {
            @Override
            public void onResponse(Call<DataClass> call, Response<DataClass> response) {
                if (response.isSuccessful()) {
                    DataClass result = response.body();

                    //서버에서 응답방은 데이터를 TextView에 넣어준다.
                    final TextView distance__sum = findViewById(R.id.distance);
                    final TextView plogging_freq = findViewById(R.id.number);

                    distance__sum.setText(result.distance__sum + "");
                    plogging_freq.setText(result.plogging_freq + "");

                } else {
                    //실패
                }
            }

            @Override
            public void onFailure(Call<DataClass> call, Throwable t) {
                //통신 실패
            }
        });
    }

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

}