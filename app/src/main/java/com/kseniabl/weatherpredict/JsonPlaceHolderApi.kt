package com.kseniabl.weatherpredict

import com.kseniabl.weatherpredict.models.Post
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// open weather map api
interface JsonPlaceHolderApi {
    companion object {
        private const val API_KEY = "b0cfe7423428e570341a2d99a8e63852"
    }

    @GET("onecall?appid=${API_KEY}")
    fun getDate(@Query("lat") lat: Double, @Query("lon") lon: Double, @Query("units") units: String): Call<Post>
}
