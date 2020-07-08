package com.example.permanentclock

import com.example.permanentclock.models.OWMWeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OWMApi {
    @GET("weather")
    fun getCurrentWeather(
        @Query("appid") appid: String,
        @Query("q") city: String,
        @Query("units") units: String
    ): Call<OWMWeatherResponse>

    @GET("weather")
    fun getCurrentWeather(
        @Query("appid") appid: String,
        @Query("lon") lon: String,
        @Query("lat") lat: String,
        @Query("units") units: String
    ): Call<OWMWeatherResponse>
}