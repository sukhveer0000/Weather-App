package com.example.weatherapp

import com.example.weatherapp.weather.Weather
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("2.5/weather")
    suspend fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appId: String,
        @Query("units")  matric: String
    ): retrofit2.Response<Weather>
}