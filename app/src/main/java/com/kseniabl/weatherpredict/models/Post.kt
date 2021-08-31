package com.kseniabl.weatherpredict.models

import com.kseniabl.weatherpredict.models.DailyData
import com.kseniabl.weatherpredict.models.HourlyData

// main model with all data
data class Post(val hourly: List<HourlyData>,
                val timezone_offset: Double,
                val daily: List<DailyData>)