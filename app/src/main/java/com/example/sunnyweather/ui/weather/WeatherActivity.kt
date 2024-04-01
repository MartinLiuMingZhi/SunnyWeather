package com.example.sunnyweather.ui.weather

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sunnyweather.MainActivity
import com.example.sunnyweather.R
import com.example.sunnyweather.databinding.ActivityWeatherBinding
import com.example.sunnyweather.databinding.ForecastBinding
import com.example.sunnyweather.databinding.ForecastHourlyBinding
import com.example.sunnyweather.databinding.LifeIndexBinding
import com.example.sunnyweather.databinding.NowBinding
import com.example.sunnyweather.databinding.PlaceManageBinding
import com.example.sunnyweather.logic.model.HourlyForecast
import com.example.sunnyweather.logic.model.PlaceManage
import com.example.sunnyweather.logic.model.Weather
import com.example.sunnyweather.logic.model.getSky
import com.example.sunnyweather.ui.place.PlaceFragment
import com.example.sunnyweather.ui.place.PlaceManageAdapter
import com.example.sunnyweather.ui.place.PlaceManageViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WeatherActivity :AppCompatActivity(){

    val viewModel by lazy { ViewModelProvider(this)[WeatherViewModel::class.java] }
    val placeManageViewModel by lazy { ViewModelProvider(this)[PlaceManageViewModel::class.java] }

    private lateinit var binding:ActivityWeatherBinding
    private lateinit var nowBinding: NowBinding
    private lateinit var forecastHourlyBinding: ForecastHourlyBinding
    private lateinit var forecastBinding: ForecastBinding
    private lateinit var lifeIndexBinding: LifeIndexBinding
    private lateinit var placeManageBinding: PlaceManageBinding
    internal lateinit var drawerLayout: DrawerLayout // 将 drawerLayout 声明为全局变量
    private lateinit var placeManageAdapter: PlaceManageAdapter

    private val hourlyForecastList = ArrayList<HourlyForecast>()
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        nowBinding = binding.nowLayout
        forecastBinding = binding.forecastLayout
        lifeIndexBinding = binding.lifeIndexLayout
        forecastHourlyBinding = binding.hourlyLayout
        placeManageBinding = binding.placeManage
        // 初始化 drawerLayout
        drawerLayout = binding.drawerLayout

//        val decorView = window.decorView
//        decorView.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                window.statusBarColor = Color.TRANSPARENT

        setContentView(binding.root)

        // 启动 PlaceFragment
        placeManageBinding.searchPlaceEntrance.setOnClickListener {
            // 创建一个 FragmentTransaction
            val transaction = supportFragmentManager.beginTransaction()

            // 创建一个新的 PlaceFragment 实例
            val fragment = PlaceFragment()

            // 将数据传递给 PlaceFragment
            val bundle = Bundle().apply {
                putString("FROM_ACTIVITY", "WeatherActivity")
            }
            fragment.arguments = bundle

            // 将 PlaceFragment 添加到 Activity 中
            transaction.replace(R.id.place_manage, fragment)
            transaction.addToBackStack(null)  // 如果需要，将该事务添加到返回栈中，以便用户可以返回前一个 Fragment
            transaction.commit()
        }


        //设置24小时预报的RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        forecastHourlyBinding.hourlyRecyclerView.layoutManager = layoutManager
        forecastHourlyBinding.hourlyRecyclerView.adapter = HourlyAdapter(hourlyForecastList)

        //设置地点管理的RecyclerView
        val layoutManager2 = LinearLayoutManager(this)
        placeManageBinding.placeManageRecyclerView.layoutManager = layoutManager2
        placeManageAdapter = PlaceManageAdapter(this,placeManageViewModel.placeManageList)
        placeManageBinding.placeManageRecyclerView.adapter = placeManageAdapter

        if (viewModel.locationLng.isEmpty()){
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
            Log.d("TAG",viewModel.locationLng)
        }

        if (viewModel.locationLat.isEmpty()){
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
            Log.d("TAG",viewModel.locationLat)
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }
        if (viewModel.placeAddress.isEmpty()) {
            viewModel.placeAddress = intent.getStringExtra("place_address") ?: ""
        }

        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()
            if (weather!=null){
                Log.d("TAG",weather.toString())
                showWeatherInfo(weather)
            }else{
                Toast.makeText(this,"无法成功获取天气信息",Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            binding.swipeRefresh.isRefreshing = false
        })

        placeManageViewModel.placeManageLiveData.observe(this) { result ->
            val placeManages = result.getOrNull()
            if (placeManages != null) {
                placeManageViewModel.placeManageList.clear()
                placeManageViewModel.placeManageList.addAll(placeManages)
               placeManageAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this,"无法获取地点管理数据", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.royal_blue)
        refreshWeather()        //刷新天气
        placeManageViewModel.refreshPlaceManage() //刷新地点管理
        
        binding.swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }
        viewModel.refreshWeather(viewModel.locationLng,viewModel.locationLat)

        nowBinding.navBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        nowBinding.addBtn.setOnClickListener {
            drawerLayout.open()
            val addPlaceManage = PlaceManage(viewModel.placeName,viewModel.locationLng,viewModel.locationLat,
                viewModel.placeAddress,viewModel.placeRealtimeTem,viewModel.placeSkycon)
            placeManageViewModel.addPlaceManage(addPlaceManage)
        }

        binding.drawerLayout.addDrawerListener(object :DrawerLayout.DrawerListener{
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {
//                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
//                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
//                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }

            override fun onDrawerStateChanged(newState: Int) {

            }

        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        drawerLayout.closeDrawers()
        viewModel.locationLng = intent?.getStringExtra("location_lng") ?: ""
        viewModel.locationLat = intent?.getStringExtra("location_lat") ?: ""
        viewModel.placeName = intent?.getStringExtra("place_name") ?: ""
        viewModel.placeAddress = intent?.getStringExtra("place_address") ?: ""
        refreshWeather()
    }

    override fun onStop() {
        super.onStop()
        placeManageViewModel.clearToast()
    }
    fun refreshWeather(){
        viewModel.refreshWeather(viewModel.locationLng,viewModel.locationLat)
        binding.swipeRefresh.isRefreshing = true
    }

    private fun showWeatherInfo(weather: Weather) {

        nowBinding.placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily
        val hourly = weather.hourly

        // 填充now.xml布局中的数据
        val realtimeTemInt = realtime.temperature.toInt()
        val currentSkyInfo = getSky(realtime.skycon).info
        val currentPM25 = realtime.airQuality.aqi.chn.toInt()
        val currentApparentTemInt = realtime.apparentTemperature.toInt()
        val currentWindDir = realtime.wind.direction
        val currentWindScale = calculateWindScale(realtime.wind.speed.toInt())
        val currentHumidity = (realtime.humidity * 100).toInt()
        nowBinding.realtimeWeather.setRealtimeWeather(realtimeTemInt,currentSkyInfo,currentPM25,
            currentApparentTemInt,currentWindDir,currentWindScale,currentHumidity)
        nowBinding.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        //将网络请求的当前温度和Skycon保存到ViewModel中
        viewModel.placeRealtimeTem = realtimeTemInt
        viewModel.placeSkycon = currentSkyInfo

        //是否是点击地点管理中的地点更新该地点温度和天气信息
        if (viewModel.isUpdatePlaceManage) {
            val updatePlaceManage = PlaceManage(viewModel.placeName,viewModel.locationLng,
                viewModel.locationLat, viewModel.placeAddress,
                viewModel.placeRealtimeTem,viewModel.placeSkycon)
            placeManageViewModel.updatePlaceManage(updatePlaceManage)
            viewModel.isUpdatePlaceManage = false
        }
        //填充forecast_hourly.xml布局中的数据
        hourlyForecastList.clear()
        for (i in 0 until hourly.skycon.size) {
            val temVal = hourly.temperature[i].value// 调整为温度值
            val skyVal = hourly.skycon[i].value
            val datetime = hourly.skycon[i].datetime // 改为datetime而不是date
            hourlyForecastList.add(HourlyForecast(temVal, skyVal, datetime))
        }
        // 更新数据后通知适配器
        (forecastHourlyBinding.hourlyRecyclerView.adapter as? HourlyAdapter)?.notifyDataSetChanged()


        // 填充forecast.xml布局中的数据
        forecastBinding.forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days){
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastBinding.forecastLayout,false)
            val dateInfo: TextView = view.findViewById(R.id.dateInfo)
            val skyIcon: ImageView = view.findViewById(R.id.skyIcon)
            val skyInfo: TextView = view.findViewById(R.id.skyInfo)
            val temperatureInfo: TextView = view.findViewById(R.id.temperatureInfo)
            val simpleDateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val dateInfoStr = when(i) {
                0 -> "今天  ${simpleDateFormat.format(skycon.date)}"
                1 -> "明天  ${simpleDateFormat.format(skycon.date)}"
                else -> "${getDayOfWeek(skycon.date)}  ${simpleDateFormat.format(skycon.date)}"
            }
            dateInfo.text = dateInfoStr
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()}"
            temperatureInfo.text = tempText
            forecastBinding.forecastLayout.addView(view)
        }

        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        lifeIndexBinding.coldRiskText.text  = lifeIndex.coldRisk[0].desc
        lifeIndexBinding.dressingText.text = lifeIndex.dressing[0].desc
        lifeIndexBinding.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        lifeIndexBinding.carWashingText.text = lifeIndex.carWashing[0].desc

        binding.weatherLayout.visibility = View.VISIBLE
    }

    private fun getDayOfWeek(date: Date): String {
        val sdf = SimpleDateFormat("E", Locale.getDefault())
        return sdf.format(date)
    }
    private fun calculateWindScale(windSpeed: Int): Int {
        return when {
            windSpeed < 1 -> 0 // Calm
            windSpeed < 6 -> 1 // Light air
            windSpeed < 12 -> 2 // Light breeze
            windSpeed < 20 -> 3 // Gentle breeze
            windSpeed < 29 -> 4 // Moderate breeze
            windSpeed < 39 -> 5 // Fresh breeze
            windSpeed < 50 -> 6 // Strong breeze
            windSpeed < 62 -> 7 // Near gale
            windSpeed < 75 -> 8 // Gale
            windSpeed < 89 -> 9 // Strong gale
            windSpeed < 103 -> 10 // Storm
            windSpeed <= 117 -> 11 // Violent storm
            else -> 12 // Hurricane
        }
    }
}