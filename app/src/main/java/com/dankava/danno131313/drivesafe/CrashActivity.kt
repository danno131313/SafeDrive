package com.dankava.danno131313.drivesafe

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_crash.*
import org.json.JSONObject

class CrashActivity : AppCompatActivity() {
    lateinit private var vibrator: Vibrator

    private var timer: CountDownTimer? = object: CountDownTimer(30000, 1000) {
        override fun onTick(time: Long) {
            vibrator.vibrate(500)
            countDownTextView.text = "" + time / 1000
            Log.d("Crash", "API will be called in " + time / 1000)
        }

        override fun onFinish() {
            runOnUiThread({
                val queue = Volley.newRequestQueue(baseContext)
                val prefs = getSharedPreferences("drivesafe", Context.MODE_PRIVATE)

                val access_token = prefs.getString("access_token", null)
                val url = "https://api-sandbox.safetrek.io/v1/alarms"

                val crashLocation = getIntent().getParcelableExtra<Location>("crashLocation")

                val lat = crashLocation.latitude
                val lng = crashLocation.longitude
                val accuracy = crashLocation.accuracy

                val jsonObj = JSONObject("{ 'location.coordinates': {" +
                        "'lat': " + lat + ", " +
                        "'lng': " + lng + ", " +
                        "'accuracy': " + accuracy.toInt() +
                        "} }")

                Log.d("JSON TO SEND", jsonObj.toString())

                var tokenRequest = object : JsonObjectRequest(Request.Method.POST, url, jsonObj,
                        Response.Listener { response ->
                            Log.d("API Create Alarm Response", response.toString())
                            val intent = Intent(baseContext, EmergencyActivity::class.java)
                            intent.putExtra("alarmId", response.get("id").toString())
                            intent.putExtra("crashLocation", crashLocation)
                            startActivity(intent)
                        },
                        Response.ErrorListener { error ->
                            Log.d("ERROR", String(error.networkResponse.data))
                        }
                ) {
                    override fun getHeaders(): MutableMap<String, String> {
                        var headers = HashMap<String, String>()
                        headers["Content-Type"] = "application/json"
                        headers["Authorization"] = "Bearer " + access_token
                        return headers
                    }
                }

                Log.d("REQUEST", tokenRequest.bodyContentType)

                queue.add(tokenRequest)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stopService(Intent(this, SensorService::class.java))
        setContentView(R.layout.activity_crash)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        crashButton.setOnClickListener {
            stopCrash()
        }

        startCrash()
    }

    override fun onBackPressed() {
        stopCrash()
    }

    private fun stopCrash() {
        SensorService.stopService = true

        timer?.cancel()
        timer = null

        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun startCrash() {
        timer?.start()
    }
}
