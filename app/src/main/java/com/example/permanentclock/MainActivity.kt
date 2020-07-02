package com.example.permanentclock

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
        const val API_KEY = "5c0a9ca8d7fbd4165b728d3bddc1d281"
        const val CITY = "11375"
        const val UNITS = "imperial"
    }

    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable

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

        // setup hourly weather update
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            getCurrentWeather()
            mHandler.postDelayed(mRunnable, 1800000) //3600000)
        }
        mHandler.post(mRunnable)
    }

    override fun onStop() {
        super.onStop()
        Log.e("PermanentClock", "PermanentClock has stopped")
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        Log.e("PermanentClock", "PermanentClock has paused")
    }

    private fun getCurrentWeather() {
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
                Log.e("PermanentClock", t.message, t)
            }
        })

    }

    private fun convertDouble(temp: Double): String {
        return temp.roundToInt().toString()
    }

    private fun updateWeatherUI(response: OWMWeatherResponse) {
        val iconView: ImageView = findViewById(R.id.weatherIcon)
        val tempView: TextView = findViewById(R.id.weatherTemp)
        val imgResource = String.format("wi_%s_2x", response.weather[0].icon)
        val res = resources.getIdentifier(imgResource, "drawable", packageName)
        iconView.setImageResource(res)
        val detailsFormat = "T: %s\u00b0\nH: %s%%\nW: %s"
        tempView.text = String.format(
            detailsFormat,
            convertDouble(response.main.temp),
            convertDouble(response.main.humidity),
            convertDouble(response.wind.speed)
        )

    }

}
