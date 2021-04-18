package com.example.rf_emfdataanalyzer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_global_comparison.*
import kotlinx.coroutines.*
import kotlin.random.Random

class GlobalComparisonActivity : AppCompatActivity() {
    private val dbGlobalRoot = FirebaseFirestore.getInstance()
    private val arrTimeToXAxis = arrayOf(
        "12A", "1A", "2A", "3A", "4A", "5A", "6A", "7A", "8A", "9A", "10A", "11A",
        "12P", "1P", "2P", "3P", "4P", "5P", "6P", "7P", "8P", "9P", "10P", "11P"
    )
    var isFetchingComplete = false
    private val mAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_comparison)
        setUpGraph()
//        loadRandomData()
        GlobalScope.launch(Dispatchers.IO) {
            val data = LineData(fetchGlobalData(),fetchMyData())
            if(isFetchingComplete)
                switchOnGraphMode(data)
        }
//        loadRandomData()
    }

    // Menu part
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_move_to_live_data, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.bt_menu_move_to_live_data) {
            startActivity(Intent(this, MainActivity::class.java))
            true
        } else
            super.onOptionsItemSelected(item)
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
        set.lineWidth = 3f
        if(global) {
            set.color = Color.MAGENTA
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
                    for (i in arrTimeToXAxis.indices) {
                        Log.d("pulkit", "global: adding $i")
                        set.addEntry(
                            Entry(
                                i.toFloat(),
                                it.getDouble(arrTimeToXAxis[i])?.toFloat() ?: 0f
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
            isFetchingComplete = true
        }
        return set
    }

    // user's own data set
    private suspend fun fetchMyData():LineDataSet {
        val set = createSet(false)

            mAuth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("pulkit",mAuth.uid!!)
                    val document = dbGlobalRoot.collection("users").document(mAuth.uid!!)
                    document.get()
                        .addOnSuccessListener {
                            if (it.exists()) {
                                for (i in arrTimeToXAxis.indices) {
                                    Log.d("pulkit", "local: adding $i")
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
            while (set.entryCount<24)
                delay(1)
            Log.d("pulkit", "local: " + set.entryCount)
        }.await()
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

    private fun switchOnGraphMode(data:LineData)
    {
        GlobalScope.launch(Dispatchers.Main) {
            graphGlobal.data = data
            data.notifyDataChanged()
            tvFetchError.visibility = View.GONE
            graphGlobal.visibility = View.VISIBLE
        }
    }
}