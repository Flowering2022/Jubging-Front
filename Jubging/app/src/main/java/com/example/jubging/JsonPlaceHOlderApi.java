package com.example.jubging;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface JsonPlaceHOlderApi {

    String MOCK_SERVER_URL	= "http://ec2-52-79-240-128.ap-northeast-2.compute.amazonaws.com/plogging/1"; // 통신 할 서버 baseUrl


    //해당 주소 경로에서 GET방식으로 가져온다.
    @GET("1")
    Call<List<Post>> getPosts();

    public class Post {
        //생성한 테이블들의 속성 예시 = userId, distance
        private int distance__sum;


        //private String test;

        public int getDistance__sum() {
            return distance__sum;
        }


//        public String getDistance() {
//            return distance;
//        }

    }

}

