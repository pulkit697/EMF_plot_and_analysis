package com.example.rf_emfdataanalyzer

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main.*

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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorEventListener = object : SensorEventListener{
            override fun onSensorChanged(event: SensorEvent?) {
                x += 0.1f
                if (event!=null)
                    addEntry(event)
                graph.data = data
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //DO NOTHING
            }
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) !=null)
        {
            tvError.visibility = View.GONE
            graph.visibility = View.VISIBLE

            setUpGraph()
            graph.data = data

            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            sensorManager.registerListener(sensorEventListener,sensor,SensorManager.SENSOR_DELAY_NORMAL)
        }
        else
        {
            graph.visibility=View.GONE
        }
    }

    private fun setUpGraph()
    {
        graph.setTouchEnabled(false)
        graph.isDragEnabled = false
        graph.setScaleEnabled(false)
        graph.setDrawGridBackground(false)
        graph.setPinchZoom(false)
        graph.setBackgroundColor(Color.WHITE)
        graph.setMaxVisibleValueCount(150)
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
        val data = graph.data
        if(data!=null) {
            var set = data.getDataSetByIndex(0)
            if(set==null) {
                set=createSet()
                data.addDataSet(set)
            }
            data.addEntry(Entry(set.entryCount.toFloat(),event.values[0]),0)
            data.notifyDataChanged()
            graph.data=data
            graph.moveViewToX(data.entryCount.toFloat())
        }
    }
}