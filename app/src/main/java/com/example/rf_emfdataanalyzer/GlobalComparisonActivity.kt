package com.example.rf_emfdataanalyzer

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_global_comparison.*

class GlobalComparisonActivity : AppCompatActivity() {
    val dbGlobalRoot = FirebaseFirestore.getInstance()
    val arrTimeToXaxis = arrayOf("12A","1A","2A","3A","4A","5A","6A","7A","8A","9A","10A","11A",
        "12P","1P","2P","3P","4P","5P","6P","7P","8P","9P","10P","11P"  )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_comparison)
        setUpGraph()
        fetchGlobalData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_move_to_live_data,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == R.id.bt_menu_move_to_live_data){
            startActivity(Intent(this,MainActivity::class.java))
            true
        } else
            super.onOptionsItemSelected(item)
    }

    private fun setUpGraph() {
        graphGlobal.setTouchEnabled(false)
        graphGlobal.isDragEnabled = false
        graphGlobal.setScaleEnabled(false)
        graphGlobal.setDrawGridBackground(false)
        graphGlobal.setPinchZoom(false)
        graphGlobal.setBackgroundColor(Color.WHITE)
        graphGlobal.setMaxVisibleValueCount(150)
    }
    private fun createSet(): LineDataSet
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

    private fun fetchGlobalData()
    {
        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        val document = dbGlobalRoot.collection("global").document("TH0qX6746EtSKFMR3UKG")
        document.get().addOnSuccessListener {
            if(it.exists()){
                var set = data.getDataSetByIndex(0)
                set = createSet()
                data.addDataSet(set)
                for(i in arrTimeToXaxis.indices)
                {
                    data.addEntry(Entry(i.toFloat(),it.getDouble(arrTimeToXaxis[i])?.toFloat()?:0f),0)
                }
                data.notifyDataChanged()
                graphGlobal.data = data
                tvFetchError.visibility = View.GONE
                graphGlobal.visibility = View.VISIBLE
            }
            else{
                Toast.makeText(this,"row is null",Toast.LENGTH_LONG).show()
                tvFetchError.text = "Sorry! some error occured"
            }
        }
            .addOnFailureListener{
                Toast.makeText(this,"failure in loading..."+it.message,Toast.LENGTH_LONG).show()
                tvFetchError.text = "Sorry! server error"
            }

    }
}