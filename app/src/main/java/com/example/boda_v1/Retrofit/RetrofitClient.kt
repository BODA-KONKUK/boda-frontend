package com.example.boda_v1.Retrofit

import android.util.Log
import com.example.boda_v1.utils.Constants.TAG
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//싱글턴 -> 메모리 1개 사용
object RetrofitClient {
    //레트로핏 클라이언트 선언
    private var retrofitClient: Retrofit? = null

    //레트로핏 클라이언트 가져오기
    fun getClient(baseUrl: String): Retrofit?{
        Log.d(TAG, "RetrofitClient - getClient() called")

        //레트로핏 클라이언트가 없다면 빌더를 통해 인스턴스 생성
        if(retrofitClient == null){
            retrofitClient = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitClient
    }

}