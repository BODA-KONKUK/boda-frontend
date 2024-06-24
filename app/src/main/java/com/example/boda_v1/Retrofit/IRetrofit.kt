package com.example.boda_v1.Retrofit

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface IRetrofit {

    // https://www.unslash.com/search/photos/?query="searchTerm"
    //@Query -> 들어가는 query
    @GET(".")
    fun helloworld(): Call<String>

    @Multipart
    @POST("upload/file")
    fun uploadImg(@Part file: MultipartBody.Part): Call<ServerMessage>

    @POST("question")
    fun sendText(@Body textData: ClientMessage): Call<ServerMessage>

    @GET("answer")
    fun getMessages(): Call<List<ServerMessage>>
}

data class TextData(val text: String)
data class ServerMessage(val message: String, val imgUrl: String?)
data class ClientMessage(val message: String, val imgUrl: String?)
data class Message(
    val text:String,
    val isUser: Boolean
    )