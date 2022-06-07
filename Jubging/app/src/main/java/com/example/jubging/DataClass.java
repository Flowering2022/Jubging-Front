package com.example.jubging;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class DataClass {
    // @SerializedName으로 일치시켜 주지않을 경우엔 클래스 변수명이 일치해야함
    @SerializedName("distance__sum")
    public int distance__sum;



    // toString()을 Override 해주지 않으면 객체 주소값을 출력함
    @Override
    public String toString() {
        return "PostResult{" +
                "distance__sum=" + distance__sum +'\'' +
                '}';
    }
}

