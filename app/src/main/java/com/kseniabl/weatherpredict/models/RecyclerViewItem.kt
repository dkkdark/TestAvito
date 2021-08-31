package com.kseniabl.weatherpredict.models

// recycler view model
data class RecyclerViewItem(
    var time: String,
    var temp: String,
    var weatherIcon: Int,
    var weatherDescription: String)