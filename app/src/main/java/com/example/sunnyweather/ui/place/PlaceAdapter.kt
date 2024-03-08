package com.example.sunnyweather.ui.place

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sunnyweather.databinding.PlaceItemBinding
import com.example.sunnyweather.logic.model.Place
import com.example.sunnyweather.ui.weather.WeatherActivity


class PlaceAdapter(private val fragment: PlaceFragment,private val placeList: List<Place>):
    RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: PlaceItemBinding) : RecyclerView.ViewHolder(binding.root) {

        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val binding = PlaceItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {

        return placeList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val place = placeList[position]
        binding.placeName.text = place.name
        binding.placeAddress.text = place.address

        holder.itemView.setOnClickListener {
            val activity = fragment.requireActivity()
            if (activity is WeatherActivity){
                activity.drawerLayout.closeDrawers()
                activity.viewModel.locationLng = place.location.lng
                activity.viewModel.locationLat = place.location.lat
                activity.viewModel.placeName = place.name
                activity.refreshWeather()
            }else{
                val intent = Intent(fragment.requireContext(),WeatherActivity::class.java).apply {
                    putExtra("location_lng",place.location.lng)
                    putExtra("location_lat",place.location.lat)
                    putExtra("place_name",place.name)
                    Log.d("TAG","adapter"+place.location)
                }
                fragment.startActivity(intent)
                fragment.requireActivity().finish()
            }
            fragment.viewModel.savePlace(place)
        }
    }

}