package com.dshu610.permanentclock

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dshu610.permanentclock.models.OWMWeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.net.Uri
import android.preference.PreferenceManager
import android.util.ArraySet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextClock
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GestureDetectorCompat
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    companion object {
        const val INTERVAL = 600000L
        const val UNITS = "imperial"
    }

    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPref: SharedPreferences
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationCallback: LocationCallback
    private lateinit var imagePaths: MutableList<Uri>
    private var imageIndex = 0
    private val PERMISSION = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
            }
        }

        setContentView(R.layout.activity_main)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(baseContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initSettingsButton()

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    initLocationRequest()
                    getLocation()
                } else {
                    Log.e("no perms", "booo")
                }
            }
        if (!hasPermissions(*PERMISSION)) {
            requestPermissions(PERMISSION, 1)
        }
        updateUIWithPrefs()

        mDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                changePhoto()
                return super.onDoubleTap(e)
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        mDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun updateUIWithPrefs() {
        val clockColor =
            sharedPref.getInt("clockTextColor", resources.getColor(R.color.clock_default, null))
        val weatherColor =
            sharedPref.getInt("weatherTextColor", resources.getColor(R.color.weather_default, null))
        val dateColor =
            sharedPref.getInt("dateTextColor", resources.getColor(R.color.date_default, null))
        val locationColor = sharedPref.getInt(
            "locationTextColor",
            resources.getColor(R.color.weather_default, null)
        )

        val clockView: TextClock = findViewById(R.id.textClock)
        val locationView: TextView? = findViewById(R.id.location)
        val weatherDetailsView: TextView = findViewById(R.id.weatherTemp)
        val dateView: TextClock = findViewById(R.id.textClockdate)

        clockView.setTextColor(clockColor)
        locationView?.setTextColor(locationColor)
        weatherDetailsView.setTextColor(weatherColor)
        dateView.setTextColor(dateColor)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val clockMinView: TextClock? = findViewById(R.id.textClockMM)
            clockMinView?.setTextColor(clockColor)
        }

        changePhoto()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun initLocationRequest() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = INTERVAL
            fastestInterval = INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                p0 ?: return
                getLocation()
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        initLocationRequest()
        initWeatherHandler()
        updateUIWithPrefs()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        Log.e("PermanentClock", "PermanentClock has paused")
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            with(sharedPref.edit()) {
                putString("lon", location?.longitude.toString())
                putString("lat", location?.latitude.toString())
                commit()
                getCurrentWeather()
            }
        }

        fusedLocationClient.lastLocation.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
    }

    private fun initSettingsButton() {
        // setup settings button
        val prefView: ImageView = findViewById(R.id.settingsButton)
        prefView.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun changePhoto() {
        val photos: Set<String> = sharedPref.getStringSet("photos", ArraySet<String>())
        if (imageIndex >= photos.size) imageIndex = 0
        if (photos.size > 0) {
            val photoView: ImageView? = findViewById(R.id.photo)
            ObjectAnimator.ofFloat(photoView, View.ALPHA, 1f, 0f).setDuration(1000).start();
            photoView?.setImageURI(null)
            photoView?.setImageURI(Uri.parse(photos.elementAt(imageIndex)))
            ObjectAnimator.ofFloat(photoView, View.ALPHA, 0f, 1f).setDuration(1000).start();
            imageIndex++
        }

    }

    private fun initWeatherHandler() {
        // setup hourly weather update
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            changePhoto()
            getCurrentWeather()
            mHandler.postDelayed(mRunnable, INTERVAL)
        }
        mHandler.post(mRunnable)
    }

    private fun getCurrentWeather() {
        val lon = sharedPref.getString("lon", "-73.8536568")
        val lat = sharedPref.getString("lat", "40.7267688")
        val unit = sharedPref.getString("unit", UNITS)

        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.owm_base_url))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: OWMApi = retrofit.create(OWMApi::class.java)
        val call = service.getCurrentWeather(getString(R.string.owm_api_key), lon, lat, unit)
        call.enqueue(object : Callback<OWMWeatherResponse> {
            override fun onResponse(
                call: Call<OWMWeatherResponse>,
                response: Response<OWMWeatherResponse>
            ) {
                if (response.isSuccessful()) {
                    updateWeatherUI(response.body()!!)
                } else {
                    Log.e("PermanentClock", response.errorBody().toString())
                }
            }

            override fun onFailure(call: Call<OWMWeatherResponse>, t: Throwable) {
                Log.e("PermanentClock", t.message, t)
            }
        })

    }

    private fun convertDouble(temp: Double): String {
        return temp.roundToInt().toString()
    }

    private fun updateWeatherUI(response: OWMWeatherResponse) {
        val locView: TextView? = findViewById(R.id.location)
        val iconView: ImageView = findViewById(R.id.weatherIcon)
        val tempView: TextView = findViewById(R.id.weatherTemp)

        val imgResource = String.format("wi_%s_2x", response.weather[0].icon)
        val res = resources.getIdentifier(imgResource, "drawable", packageName)
        iconView.setImageResource(res)
        locView?.text = response.name
        val detailsFormat = "T:%s\u00b0\nH:%s%%\nW:%s"
        tempView.text = String.format(
            detailsFormat,
            convertDouble(response.main.temp),
            convertDouble(response.main.humidity),
            convertDouble(response.wind.speed)
        )

    }

}
