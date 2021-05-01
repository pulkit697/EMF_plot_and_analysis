package com.example.rf_emfdataanalyzer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.work.*
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    lateinit var sensorManager: SensorManager
    lateinit var sensor: Sensor
    lateinit var sensorEventListener: SensorEventListener
    var x=0f
    var plotEntry = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val data = LineData()
        data.setValueTextColor(Color.WHITE)

        btStartSensing.setOnClickListener {
            startSensing()
        }



        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorEventListener = object : SensorEventListener{
            override fun onSensorChanged(event: SensorEvent?) {
                x += 0.1f
                if (event!=null)
                    addEntry(event)
                graphLive.data = data
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //DO NOTHING
            }
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) !=null)
        {
            tvError.visibility = View.GONE
            graphLive.visibility = View.VISIBLE

            setUpGraph()
            graphLive.data = data

            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensorManager.registerListener(sensorEventListener,sensor,SensorManager.SENSOR_DELAY_NORMAL)
        }
        else
        {
            graphLive.visibility=View.GONE
        }
    }

    private fun startSensing() {
        /* start service here*/
        val workRequest = PeriodicWorkRequestBuilder<SaveDataJobService>(30,TimeUnit.MINUTES)
            .setInitialDelay(0,TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("emf work",ExistingPeriodicWorkPolicy.KEEP,workRequest)
        val workRequestUpload = PeriodicWorkRequestBuilder<UploadDataWorkClass>(1,TimeUnit.HOURS)
            .setInitialDelay(0,TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("uploading work",ExistingPeriodicWorkPolicy.KEEP,workRequestUpload)
        val pref = getSharedPreferences(SHARED_PREF_NAME,Context.MODE_PRIVATE)
        pref.edit().putBoolean("SensingStarted",true).apply()
        btStartSensing.isEnabled = false
    }

    private fun isSensingStarted():Boolean {
        val pref = getSharedPreferences(SHARED_PREF_NAME,Context.MODE_PRIVATE)
        return pref.getBoolean("SensingStarted",false)
    }

    // Menu part
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_move_to_live_data, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.bt_menu_move_to_live_data) {
            startActivity(Intent(this, GlobalComparisonActivity::class.java))
            true
        } else
            super.onOptionsItemSelected(item)
    }

    private fun setUpGraph()
    {
        graphLive.setTouchEnabled(false)
        graphLive.isDragEnabled = false
        graphLive.setScaleEnabled(false)
        graphLive.setDrawGridBackground(false)
        graphLive.setPinchZoom(false)
        graphLive.setBackgroundColor(Color.WHITE)
        graphLive.setMaxVisibleValueCount(150)
    }

    private fun createSet():LineDataSet
    {
        val set = LineDataSet(null,"Dynamic DataSet")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 3f
        set.color = Color.MAGENTA
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        set.setDrawCircles(false)
        set.setDrawCircleHole(false)
        set.setDrawValues(false)
        return set
    }

    private fun addEntry(event: SensorEvent)
    {
        val data = graphLive.data
        if(data!=null) {
            var set = data.getDataSetByIndex(0)
            if(set==null) {
                set=createSet()
                data.addDataSet(set)
            }
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val sensorData= sqrt(x*x + y*y + z*z)
            data.addEntry(Entry(set.entryCount.toFloat(),sensorData),0)
            data.notifyDataChanged()
            graphLive.data=data
            graphLive.moveViewToX(data.entryCount.toFloat())
        }
    }
}