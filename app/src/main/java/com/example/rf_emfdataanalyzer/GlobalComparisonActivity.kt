package com.example.rf_emfdataanalyzer

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_global_comparison.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class GlobalComparisonActivity : AppCompatActivity() {
    private val dbGlobalRoot = FirebaseFirestore.getInstance()

    var isFetchingComplete = false
    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_comparison)
        setUpGraph()
        GlobalScope.launch(Dispatchers.IO) {
            val data = LineData(fetchGlobalData(),fetchMyData())
            if(isFetchingComplete)
                switchOnGraphMode(data)
        }
//        fetchSharedPrefs()
//        loadRandomData()
    }


    // UI setup
    private fun setUpGraph() {
        graphGlobal.setTouchEnabled(false)
        graphGlobal.isDragEnabled = false
        graphGlobal.setScaleEnabled(false)
        graphGlobal.setDrawGridBackground(false)
        graphGlobal.setPinchZoom(false)
        graphGlobal.setBackgroundColor(Color.WHITE)
        graphGlobal.setMaxVisibleValueCount(150)
    }

    private fun createSet(global:Boolean): LineDataSet {
        val set = LineDataSet(null, "" + if(global) "Global data" else "Your data")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = if(global)
                            1.67f
                        else
                            3f
        if(global) {
            set.color = Color.argb(152,255,0,255)
            set.fillColor = Color.GREEN
        }
        else {
            set.color = Color.BLUE
            set.fillColor = Color.RED
        }

        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        set.setDrawCircles(false)
        set.setDrawCircleHole(false)
        set.setDrawValues(false)
        return set
    }

    // global data
    private suspend fun fetchGlobalData():LineDataSet {
        val set = createSet(true)
        val document =
            dbGlobalRoot.collection("global").document("TH0qX6746EtSKFMR3UKG")
            document.get().addOnSuccessListener {
                if (it.exists()) {
                    Log.d("pulkit","global: $it")
                    for (i in arrTimeToXAxis.indices) {
                        Log.d("pulkit", "global: adding $i    ${it.getDouble(arrTimeToXAxis[i])}")
                        set.addEntry(
                            Entry(
                                i.toFloat(),
                                it.getDouble(arrTimeToXAxis[i])?.toFloat() ?: 50f
                            ))
                    }
                } else {
                    Log.d("pulkit", "global: row is null")
                    tvFetchError.text = getString(R.string.empty_row)
                }
            }
                        .addOnFailureListener {
                    Log.d("pulkit", "global: on failure listener")
                    tvFetchError.text = getString(R.string.fetch_failed_error)
                }
        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            while (set.entryCount < 24) {
                delay(1)
            }
            Log.d("pulkit", "global: " + set.entryCount)
        }
        return set
    }

    // user's own data set
    private suspend fun fetchMyData():LineDataSet {
        val set = createSet(false)
        val map = HashMap<String,Float>()
            mAuth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("pulkit",mAuth.uid!!)
                    val document = dbGlobalRoot.collection(mAuth.uid!!).document("average")
                    document.get()
                        .addOnSuccessListener {
                            Log.d("pulkit","local: $it")
                            if (it.exists()) {
                                for (i in arrTimeToXAxis.indices) {
                                    Log.d("pulkit", "local: adding $i    ${it.getDouble(arrTimeToXAxis[i])}")
                                    set.addEntry(
                                        Entry(
                                            i.toFloat(),
                                            it.getDouble(arrTimeToXAxis[i])?.toFloat() ?: 0f
                                        )
                                    )
                                }
                            } else {
                                Log.d("pulkit", "local: row is null")
                            }
                        }
                        .addOnFailureListener {
                            Log.d("pulkit", "local: on failure listener")
                        }
                } else
                    Log.d("pulkit", "local: task unsuccessful")
            }
        GlobalScope.async(Dispatchers.IO)
        {
            while (set.entryCount<48)
                delay(1)
            Log.d("pulkit", "local: " + set.entryCount)
        }.await()
        isFetchingComplete = true
        return set
    }

    private fun loadRandomData(){
        mAuth.signInAnonymously().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val mp = HashMap<String,Double>()
                for(i in arrTimeToXAxis.indices)
                {
                    mp[arrTimeToXAxis[i]] = Random.nextDouble()
                }
                dbGlobalRoot.collection("users").document(mAuth.uid!!).set(mp)
            } else
                Toast.makeText(this, "error getting your data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchOnGraphMode(data:LineData) {
        GlobalScope.launch(Dispatchers.Main) {
            graphGlobal.data = data
            data.notifyDataChanged()
            tvFetchError.visibility = View.GONE
            svSharedPrefScroll.visibility = View.GONE
            graphGlobal.visibility = View.VISIBLE
        }
    }

    private fun fetchSharedPrefs() {
        val pref = getSharedPreferences(SHARED_PREF_NAME,Context.MODE_PRIVATE)
        var str = ""
        for(times in arrTimeToXAxis)
        {
            str+=(pref.getFloat(times,0f).toString()+"\n")
        }
        tvFetchError.text = str
    }


}