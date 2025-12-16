package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private var LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this@MainActivity, "Permission is granted", Toast.LENGTH_SHORT)
                    .show()
                checkLocationAndFetch()
            } else if (shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
                Toast.makeText(
                    this,
                    "Location permission is required for weather",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showPermissionDialog()
            }
        }

        checkForLocationPermission()

    }

    private fun checkForLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
           checkLocationAndFetch()
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSION)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission needed")
            .setMessage("This permission is needed for accessing the nearby location temperature.")
            .setPositiveButton("Go To Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("close", null)
            .show()

    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                LOCATION_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

//                    Toast.makeText(
//                        this@MainActivity,
//                        "Latitude = $latitude & Longitude = $longitude",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    getLocationWeatherDetails(latitude, longitude)
                } else {
                    Toast.makeText(this@MainActivity, "Location Not Found", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun checkLocationAndFetch() {
        if (!isLocationEnabled()) {
            Toast.makeText(this@MainActivity, "Turn on location", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            getCurrentLocation()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {

            lifecycleScope.launch {

                val response = try {
                    RetrofitInstance.api.getWeather(
                        latitude,
                        longitude,
                        Constants.APP_ID,
                        Constants.UNITS
                    )
                } catch (e: IOException) {
                    Log.d("tag", e.toString())
                    return@launch
                } catch (e: HttpException) {
                    Log.d("tag", e.toString())
                    return@launch
                }

                if (response.isSuccessful) {
                    val weather = response.body()!!
                    Log.d("tag", weather.toString())
//                    Toast.makeText(this@MainActivity, weather.toString(), Toast.LENGTH_SHORT).show()

                    binding.tvCity.text = weather.name
                    binding.tvMainTemp.text = "${weather.main.temp}\u00B0C"
                    binding.tvMinTemp.text = "${ weather.main.temp_min }\u00B0C"
                    binding.tvMaxTemp.text = "${ weather.main.temp_max }\u00B0C"
                    binding.tvSunsetTime.text = convertTime(weather.sys.sunset.toLong())
                    binding.tvSunriseTime.text = convertTime(weather.sys.sunrise.toLong())
                    binding.tvWindSpeed.text = "${ weather.wind.speed } m/s"
                    binding.tvPressure.text = "${weather.main.pressure} hPa"
                    binding.tvHumidity.text = "${weather.main.humidity} %"



                } else {
                    Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }


            }
        } else {
            Toast.makeText(this@MainActivity, "There is no Network", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertTime(time: Long): String{
        val date = Date(time*1000L)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return  sdf.format(date)
    }
}





