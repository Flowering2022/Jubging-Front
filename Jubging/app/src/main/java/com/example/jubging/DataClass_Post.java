package com.example.jubging;

import com.google.gson.annotations.SerializedName;

import java.sql.Time;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class DataClass_Post {

    @SerializedName("userid")
    private int userid;
    @SerializedName("distance")
    private int distance;
    @SerializedName("runningtime")
    private String runningtime;

    public DataClass_Post(int a, int b, String str){
        this.userid = a;
        this.distance = b;
        this.runningtime = str;
    }


    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getRunningtime() {
        return runningtime;
    }

    public void setRunningtime(String runningtime) {
        this.runningtime = runningtime;
    }
    //private boolean isRight = false;
    //POST
//    @SerializedName("userid")
//    public int userid;
//
//    @SerializedName("distance")
//    public int distance;
//
//    @SerializedName("runningtime")
//    public String runningtime;

//    public boolean isRight() {
//        return isRight;
//    }
//
//    public void setRight(boolean right) {
//        isRight = right;
//    }



    //POST
    // 서버에 추가할 PostResult 객체 생성, id는 서버에서 자동으로 번호 지정되는 값이기에 설정x
//GET
    // toString()을 Override 해주지 않으면 객체 주소값을 출력함
//    @Override
//    public String toString() {
//        return "PostResult{" +
//                "userid=" + userid +
//                "distance" + distance +
//                "runningtime" + runningtime +
//                '\'' +
//                '}';
//    }


}
