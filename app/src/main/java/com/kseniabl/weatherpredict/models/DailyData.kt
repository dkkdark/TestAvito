package com.kseniabl.weatherpredict.models

// // model of forecast for week
data class DailyData(
        val dt: Int,
        val temp: TempStages,
        val weather: List<WeatherData>
)