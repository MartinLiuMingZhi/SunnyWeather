package com.example.sunnyweather.ui.weather

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sunnyweather.R
import com.example.sunnyweather.databinding.ForecastHourlyItemBinding
import com.example.sunnyweather.databinding.PlaceItemBinding
import com.example.sunnyweather.logic.model.HourlyForecast
import com.example.sunnyweather.logic.model.getSky
import java.text.SimpleDateFormat
import java.util.Locale

class HourlyAdapter(private val hourlyForecastList: List<HourlyForecast>): RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    inner class ViewHolder(val binding:ForecastHourlyItemBinding): RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ForecastHourlyItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return hourlyForecastList.size
    }

//    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val hourlyForecast = hourlyForecastList[position]
        val hourlyTemperatureInfoText = hourlyForecast.temVal.toInt()
        binding.hourlyTemperatureInfo.text = "${hourlyTemperatureInfoText}°"
        val sky = getSky(hourlyForecast.skyVal)
        binding.hourlySkyIcon.setImageResource(sky.icon)
        val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.hourlyDateInfo.text = simpleDateFormat.format(hourlyForecast.datetime)
    }
}