package com.example.sunnyweather.logic.network

import com.example.sunnyweather.SunnyWeatherApplication
import com.example.sunnyweather.logic.model.DailyResponse
import com.example.sunnyweather.logic.model.RealtimeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WeatherService {

    @GET("v2.6/${SunnyWeatherApplication.TOKEN}/{lng},{lat}/realtime")
    fun getRealtimeWeather(@Path("lng") lng:String,@Path("lat") lat:String):Call<RealtimeResponse>


    @GET("v2.6/${SunnyWeatherApplication.TOKEN}/{lng},{lat}/daily")
    fun getDailyWeather(@Path("lng") lng:String,@Path("lat") lat: String):Call<DailyResponse>


}