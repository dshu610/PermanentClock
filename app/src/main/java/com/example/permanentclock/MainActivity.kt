package com.example.permanentclock

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.permanentclock.models.OWMWeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        const val IMG_URL = "https://openweathermap.org/img/wn/%s@2x.png"
        const val API_KEY = "5c0a9ca8d7fbd4165b728d3bddc1d281"
        const val CITY = "11375"
        const val UNITS = "imperial"
    }

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                acquire()
            }
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }


        setContentView(R.layout.activity_main)

        // setup hourly weather update
        mHandler = Handler()
        mRunnable = Runnable {
            getCurrentWeather()
            mHandler.postDelayed(mRunnable, 3600000)
        }
        mHandler.post(mRunnable)
    }

    override fun onStop() {
        super.onStop()
        wakeLock.release()
    }

    override fun onStart() {
        super.onStart()
        wakeLock.acquire()
    }

    internal fun getCurrentWeather() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service: OWMApi = retrofit.create(OWMApi::class.java)
        val call = service.getCurrentWeather(API_KEY, CITY, UNITS)
        call.enqueue(object : Callback<OWMWeatherResponse> {
            override fun onResponse(
                call: Call<OWMWeatherResponse>,
                response: Response<OWMWeatherResponse>
            ) {
                updateWeatherUI(response.body()!!)
            }

            override fun onFailure(call: Call<OWMWeatherResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })

    }

    fun convertDouble(temp: Double): String {
        return temp.roundToInt().toString()
    }

    fun updateWeatherUI(response: OWMWeatherResponse) {
        val iconView: ImageView = findViewById(R.id.weatherIcon)
        val tempView: TextView = findViewById(R.id.weatherTemp)

        val imgUrl = String.format(IMG_URL, response.weather[0].icon)
        Glide.with(this)
            .load(imgUrl)
            .error(R.drawable.placeholder)
            .into(iconView)
        val detailsFormat = "%s\u00b0 (%s°)   H: %s%%   W: %s"
        tempView.text = String.format(
            detailsFormat,
            convertDouble(response.main.temp),
            convertDouble(response.main.feels_like),
            convertDouble(response.main.humidity),
            convertDouble(response.wind.speed)
        )

    }

}
