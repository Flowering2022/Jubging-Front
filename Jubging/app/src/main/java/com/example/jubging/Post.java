package com.example.jubging;

import com.google.gson.annotations.SerializedName;

public class Post {
    // @SerializedName으로 일치시켜 주지않을 경우엔 클래스 변수명이 일치해야함

    public int distance__sum;
    public int plogging_freq;

    public int getDistance__sum() {
        return distance__sum;
    }

    public void setDistance__sum(int distance__sum) {
        this.distance__sum = distance__sum;
    }


    public int getPlogging_freq() {
        return plogging_freq;
    }

    public void setPlogging_freq(int plogging_freq) {
        this.plogging_freq = plogging_freq;
    }

    // toString()을 Override 해주지 않으면 객체 주소값을 출력함
    @Override
    public String toString() {
        return "PostResult{" +
                "distance__sum=" + distance__sum +
                "plogging_freq" + plogging_freq +
                '\'' +
                '}';
    }



}
