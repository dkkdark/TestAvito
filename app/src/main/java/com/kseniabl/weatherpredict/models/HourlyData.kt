package com.kseniabl.weatherpredict.models

// model of forecast for today
data class HourlyData(
    var dt: Int,
    var temp: Double,
    var weather: List<WeatherData>)