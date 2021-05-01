package com.example.rf_emfdataanalyzer

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.sqrt

class SaveDataJobService(private val context: Context, val params: WorkerParameters):CoroutineWorker(context,params) {
    lateinit var sensorManager: SensorManager
    lateinit var listener: SensorEventListener
    var dataFetched = false
    var sensorData=0f
    override suspend fun doWork(): Result {
        Log.d("pulkit","work manager called")
        withContext(Dispatchers.IO)
        {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            listener = object : SensorEventListener{
                override fun onSensorChanged(event: SensorEvent?) {
                    if(event!=null){
                        if(!dataFetched) {
                            val calendar = Calendar.getInstance()
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]
                            sensorData= sqrt(x*x + y*y + z*z)
                            dataFetched = true
                            val pref =
                                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
                            val editor = pref.edit()
                            Log.d("pulkit",""+sensorData)
                            editor.putFloat(arrTimeToXAxis[calendar.get(Calendar.HOUR_OF_DAY)*2 + calendar.get(Calendar.MINUTE)/30], sensorData)
                            editor.apply()
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    /**/
                }
            }
            sensorManager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_NORMAL)
            delay(2000)
        }
        return if (dataFetched) {
            sensorManager.unregisterListener(listener)
            Result.success()
        }
        else {
            Result.failure()
        }
    }
}