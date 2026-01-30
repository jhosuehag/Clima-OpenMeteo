package com.jhosue.weather.extreme.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jhosue.weather.extreme.R
import com.jhosue.weather.extreme.presentation.components.WeatherUtils

// Lightweight DTO for Widget
data class WidgetWeatherItem(
    val locationName: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val description: String // Pre-resolved description
)

class WeatherWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WeatherRemoteViewsFactory(this.applicationContext)
    }

    class WeatherRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {

        private var weatherItems: List<WidgetWeatherItem> = emptyList()
        private val gson = Gson()

        override fun onCreate() {
            // Initial load
        }

        override fun onDataSetChanged() {
            try {
                // Reading from SharedPreferences
                val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                val jsonDiff = prefs.getString("all_locations_data", null)

                if (jsonDiff != null) {
                    val type = object : TypeToken<List<WidgetWeatherItem>>() {}.type
                    weatherItems = gson.fromJson(jsonDiff, type) ?: emptyList()
                } else {
                    weatherItems = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                weatherItems = emptyList()
            }
        }

        override fun onDestroy() {
            weatherItems = emptyList()
        }

        override fun getCount(): Int {
            return weatherItems.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            // CRITICAL: Block try-catch for robustness
            try {
                if (position == -1 || position >= weatherItems.size) {
                    return RemoteViews(context.packageName, R.layout.widget_list_item)
                }

                val item = weatherItems[position]
                val views = RemoteViews(context.packageName, R.layout.widget_list_item)

                // 1. Bind Data with Null Safety / Defaults
                val name = item.locationName ?: "Desconocido"
                val temp = item.temperature ?: 0.0
                val desc = item.description ?: ""
                val code = item.weatherCode ?: 0
                val isDay = item.isDay ?: true

                views.setTextViewText(R.id.widget_item_location, name)
                views.setTextViewText(R.id.widget_item_temp, "${temp.toInt()}°")
                views.setTextViewText(R.id.widget_item_description, desc)

                // 2. Resolve Icon
                // Assuming WeatherUtils is available and static. 
                // If it crashes, catch block will handle it.
                val iconRes = WeatherUtils.getIconResourceForWeatherCode(code, isDay)
                views.setImageViewResource(R.id.widget_item_icon, iconRes)

                // 3. Fill Intent for Click
                val fillInIntent = Intent()
                // You can put extra data here if needed to open specific location
                // fillInIntent.putExtra("location_name", name)
                views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

                return views

            } catch (e: Exception) {
                e.printStackTrace()
                // Return a placeholder or error view if possible, or just a standard safety view
                val errorView = RemoteViews(context.packageName, R.layout.widget_list_item)
                errorView.setTextViewText(R.id.widget_item_location, "Error loading")
                return errorView
            }
        }

        override fun getLoadingView(): RemoteViews? {
            // Return null to use default loading view or create a custom one
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }
    }
}