package com.example.sunnyweather.logic


import androidx.lifecycle.liveData
import com.example.sunnyweather.SunnyWeatherApplication
import com.example.sunnyweather.logic.dao.PlaceDao
import com.example.sunnyweather.logic.model.Place
import com.example.sunnyweather.logic.model.PlaceManage
import com.example.sunnyweather.logic.model.Weather
import com.example.sunnyweather.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.Exception
import kotlin.RuntimeException
import kotlin.coroutines.CoroutineContext

object Repository {
    private val placeManageDao = AppDatabase.getDatabase(SunnyWeatherApplication.context).placeManageDao()
    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()
    fun searchPlaces(query:String) = fire(Dispatchers.IO){
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok"){
            val places = placeResponse.places
            Result.success(places)
        }else{
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    fun refreshWeather(lng:String,lat:String) = fire(Dispatchers.IO){
        coroutineScope{
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng,lat)
            }
            val deferredDaily = async{
                SunnyWeatherNetwork.getDailyWeather(lng,lat)
            }
            val deferredHourly = async {
                SunnyWeatherNetwork.getHourlyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val hourlyResponse = deferredHourly.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime,
                    hourlyResponse.result.hourly,
                    dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "hourly response status is ${hourlyResponse.status}" +
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    suspend fun addPlaceManage(placeManage: PlaceManage): Result<Int> {
        return try {
            withContext(Dispatchers.IO) {
                val queryPlaceManage = async { placeManageDao.querySpecifyPlaceManage(placeManage.lng,placeManage.lat) }.await()
                if (queryPlaceManage == null) {
                    async { placeManageDao.insertPlaceManage(placeManage) }.await()
                } else {
                    queryPlaceManage.realtimeTem = placeManage.realtimeTem
                    queryPlaceManage.skycon = placeManage.skycon
                    async { placeManageDao.updatePlaceManage(queryPlaceManage)}.await()
                }
                Result.success(1)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaceManage(placeManage: PlaceManage): Result<Int> {
        return try {
            withContext(Dispatchers.IO) {
                val queryPlaceManage = async { placeManageDao.querySpecifyPlaceManage(placeManage.lng,placeManage.lat) }.await()
                queryPlaceManage?.realtimeTem = placeManage.realtimeTem
                queryPlaceManage?.skycon = placeManage.skycon
                async { placeManageDao.updatePlaceManage(queryPlaceManage!!)}.await()
                Result.success(1)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaceManage(lng: String, lat: String): Result<Int> {
        return try {
            withContext(Dispatchers.IO) {
                val num = async { placeManageDao.deletePlaceManageByLngLat(lng, lat) }.await()
                Result.success(num)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadAllPlaceManages() = fire(Dispatchers.IO) {
        coroutineScope {
            val placeManageList = async { placeManageDao.loadAllPlaceManages() }.await()
            Result.success(placeManageList)
        }
    }
    private fun <T> fire(context:CoroutineContext,block:suspend () -> Result<T>) = liveData<Result<T>>(context){
        val result = try {
            block()
        }catch (e:Exception){
            Result.failure<T>(e)
        }
        emit(result)
    }


//    fun searchPlaces(query: String) = liveData(Dispatchers.IO){
//        val result = try {
//            val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
//            if (placeResponse.status == "ok"){
//                val places = placeResponse.places
//                Result.success(places)
//            }else{
//                Result.failure(RuntimeException("response status is ${placeResponse.status}"))
//            }
//        }catch (e:Exception){
//            Result.failure<List<Place>>(e)
//        }
//        emit(result)
//    }
}