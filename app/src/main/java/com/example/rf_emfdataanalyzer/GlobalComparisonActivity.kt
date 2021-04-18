package com.example.rf_emfdataanalyzer

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*

class GlobalComparisonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_comparison)

        setUpGraph()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_move_to_live_data,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == R.id.bt_menu_move_to_live_data){
            Intent(this,MainActivity::class.java)
            true
        } else
            super.onOptionsItemSelected(item)
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
}