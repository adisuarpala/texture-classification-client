package com.example.textureclassification.Retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofitClient=null;

    public static Retrofit getClient(){
        if (retrofitClient == null){
            retrofitClient = new Retrofit.Builder()
                    .baseUrl("http://192.168.1.3:5000") // 192.168.1.3:5000 localhost pada pc >MainActivity.java
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofitClient;
    }
}
