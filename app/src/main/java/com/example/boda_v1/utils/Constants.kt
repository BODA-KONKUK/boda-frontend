package com.example.boda_v1.utils

object Constants{
    const val TAG : String = "로그"
}

enum class SEARCH_TYPE {
    PHOTO,
    USER
}

enum class RESPONSE_STATE {
    OKAY,
    FAIL
}

object API {
    const val BASE_URL : String = "http://3.34.232.60:3000/"

    const val CLIENT_ID : String = ""

    const val UPLOAD_FILE : String = "upload/file"
    const val SEARCH_USERS : String = "search/users"



}