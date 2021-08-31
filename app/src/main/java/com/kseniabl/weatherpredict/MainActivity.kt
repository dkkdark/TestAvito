package com.kseniabl.weatherpredict

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.kseniabl.weatherpredict.models.DailyData
import com.kseniabl.weatherpredict.models.HourlyData
import com.kseniabl.weatherpredict.models.Post
import com.kseniabl.weatherpredict.models.RecyclerViewItem
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private val listForRecyclerView = arrayListOf<RecyclerViewItem>()
    // saved first time of forecast for today
    private var firstTime = 0.0

    // saved lat and lon
    private var latNow = 0.0
    private var lonNow = 0.0

    private val locationRequest: LocationRequest = LocationRequest.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000

        // try get location when user open app
        getCurrentLocation()

        Places.initialize(applicationContext, PLACES_API_KEY)

        // for choose cities by search uses google places api
        // set cities filter
        choose_place.setOnClickListener {
            val fieldList = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).setTypeFilter(
                TypeFilter.CITIES
            ).build(this)
            activityResult.launch(intent)
        }

        // get location by click on location image
        location_image.setOnClickListener { getCurrentLocation() }

        // switch between forecast for today and for week
        switch_time.setOnClickListener {
            if (switch_time.text ==  getString(R.string.see_forecast_for_week)) {
                showWeather(latNow, lonNow, 1)
                switch_time.text = getString(R.string.see_forecast_for_today)
                today_text.text = getString(R.string.forecast_for_week)
            }
            else {
                showWeather(latNow, lonNow, 0)
                switch_time.text = getString(R.string.see_forecast_for_week)
                today_text.text = getString(R.string.forecast_for_today)
            }

        }
    }

    // "activityResult" for places
    var activityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = result.data?.let { Autocomplete.getPlaceFromIntent(it) }
            choose_place.setText(place?.name)
            if (place?.latLng?.latitude != null && place.latLng?.longitude != null) {
                // call showWeather after getting city's latitude and longitude
                latNow = place.latLng!!.latitude
                lonNow = place.latLng!!.longitude

                // show forecast for today or for week
                if (switch_time.text == getString(R.string.see_forecast_for_week))
                    showWeather(place.latLng!!.latitude, place.latLng!!.longitude, 0)
                else
                    showWeather(place.latLng!!.latitude, place.latLng!!.longitude, 1)
            }
        }
    }

    private fun showWeather(lat: Double, lon: Double, ident: Int) {
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val call = retrofit.create(JsonPlaceHolderApi::class.java)

        call.getDate(lat, lon, "metric").enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful)
                    Log.e("ERROR_TAG", "Error: ${response.code()}")

                listForRecyclerView.clear()

                if (ident == 0) {
                    // get hourly forecast weather data
                    val post = response.body()
                    val hourly = post?.hourly

                    // count user's utc offset
                    val tz = TimeZone.getDefault()
                    val now = Date()
                    val offsetFromUtc = tz.getOffset(now.time) / 1000

                    // get first time from list and count how many lines we need until day is over
                    // otherwise it will be forecast for twenty four hours
                    if (post?.hourly?.get(0)?.dt != null)
                        firstTime = post.hourly[0].dt - offsetFromUtc + post.timezone_offset
                    val sdf = SimpleDateFormat("HH:mm")
                    val normalDate = sdf.format(Date(firstTime.toLong() * 1000))
                    val hours = normalDate.substringBefore(":").toInt()
                    val stringsCount = 24 - hours

                    var count = 0
                    if (hourly != null) {
                        for (el in hourly) {
                            if (count < stringsCount) {
                                count += 1
                                gatherDataForToday(el, post.timezone_offset, offsetFromUtc)
                            } else
                                break
                        }
                    }

                }

                if (ident == 1) {
                    // get daily forecast
                    val post = response.body()
                    val daily = post?.daily

                    if (daily != null) {
                        for (el in daily) {
                            gatherDataForWeek(el)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Log.e("ERROR_TAG", "Error: ${t.message}")
            }
        })
    }

    private fun gatherDataForToday(element: HourlyData, offset: Double, offsetFromUtc: Int) {
        // convert date from unix to normal view
        val longDate = element.dt - offsetFromUtc + offset
        val sdf = SimpleDateFormat("HH:mm")
        val normalDate = sdf.format(Date(longDate.toLong() * 1000))

        // get temperature
        val normalTemp = element.temp.roundToInt()

        // get icon
        val icon = "ic${element.weather[0].icon}2x"
        val drawableIcon = resources.getIdentifier(
            "com.kseniabl.weatherpredict:drawable/$icon",
            null,
            null
        )

        // get weather description
        val weatherDescr = element.weather[0].main

        // create recycler view
        val linearLayout = LinearLayoutManager(this)
        weather_recycler_view.layoutManager = linearLayout
        listForRecyclerView.add(
            RecyclerViewItem(
                normalDate,
                normalTemp.toString(),
                drawableIcon,
                weatherDescr
            )
        )
        val recyclerAdapter = RecyclerViewAdapter(listForRecyclerView, this)
        weather_recycler_view.adapter = recyclerAdapter
        recyclerAdapter.notifyDataSetChanged()
    }

    private fun gatherDataForWeek(element: DailyData) {
        val longDate = element.dt
        val sdf = SimpleDateFormat("dd.MM.yyyy")
        var normalDate = sdf.format(Date(longDate.toLong() * 1000))

        // get today's date in string view
        val c = Calendar.getInstance().time
        val currentDate = sdf.format(c)

        //get tomorrow's date in string view
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.time
        val tomorrowDate = sdf.format(tomorrow)

        // if date = today's date transform normalDate to "Сегодня". With tomorrow's date do the same
        if (sdf.parse(normalDate) == sdf.parse(currentDate)) {
            normalDate = getString(R.string.today)
        }
        else if (sdf.parse(normalDate) == sdf.parse(tomorrowDate)) {
            normalDate = getString(R.string.tomorrow)
        }


        val normalTemp = element.temp.day.roundToInt()

        val icon = "ic${element.weather[0].icon}2x"
        val drawableIcon = resources.getIdentifier(
                "com.kseniabl.weatherpredict:drawable/$icon",
                null,
                null
        )

        val weatherDescr = element.weather[0].main

        val linearLayout = LinearLayoutManager(this)
        weather_recycler_view.layoutManager = linearLayout
        listForRecyclerView.add(
                RecyclerViewItem(
                        normalDate,
                        normalTemp.toString(),
                        drawableIcon,
                        weatherDescr
                )
        )
        val recyclerAdapter = RecyclerViewAdapter(listForRecyclerView, this)
        weather_recycler_view.adapter = recyclerAdapter
        recyclerAdapter.notifyDataSetChanged()
    }

    // methods below for determine user's location

    // get location and determine user's city, then call showWeather
    private fun getCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(this)
                        .requestLocationUpdates(locationRequest, object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                super.onLocationResult(locationResult)
                                LocationServices.getFusedLocationProviderClient(this@MainActivity)
                                    .removeLocationUpdates(
                                        this
                                    )

                                if (locationResult.locations.size > 0) {
                                    val index = locationResult.locations.size - 1
                                    val latitude = locationResult.locations[index].latitude
                                    val longitude = locationResult.locations[index].longitude

                                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                                    val address = geocoder.getFromLocation(latitude, longitude, 1)
                                    val cityName = address[0].locality
                                    choose_place.text = cityName

                                    latNow = latitude
                                    lonNow = longitude
                                    showWeather(latitude, longitude, 0)

                                }
                            }
                        }, Looper.getMainLooper())
                } else {
                    turnOnGPS()
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled())
                    getCurrentLocation()
                else
                    turnOnGPS()
            }
        }
    }

    private fun isGPSEnabled(): Boolean {
        var locationManager: LocationManager? = null
        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun turnOnGPS() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.getSettingsClient(applicationContext)
            .checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                Toast.makeText(this, "GPS is already turned on", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Log.e("ERROR_TAG", "Error: $e") }

        }
    }

    companion object {
        private const val PLACES_API_KEY = "AIzaSyADivrWFGpBgEj8szDde7e6-F84Kg1I0Jo"
    }

}