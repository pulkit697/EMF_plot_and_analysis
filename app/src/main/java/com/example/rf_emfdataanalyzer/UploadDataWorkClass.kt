package com.example.rf_emfdataanalyzer

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class UploadDataWorkClass(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    var flag=false
    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO)
        {
            val mAuth = FirebaseAuth.getInstance()
            Log.d("job","" + mAuth.uid)
            val dbGlobalRoot = FirebaseFirestore.getInstance()
            val prefs =
                applicationContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            mAuth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("job","task successful")
                    val previousMap = HashMap<String,Float>()
                    dbGlobalRoot.collection(mAuth.uid!!).document("average").get()
                            .addOnSuccessListener {
                                if(it.exists())
                                {
                                    Log.d("job","inside it exists")
                                    for(times in arrTimeToXAxis)
                                    {
                                        previousMap[times] = it.getDouble(times)?.toFloat()?:0f
                                    }
                                }
                                else
                                {
                                    Log.d("job"," null of it.exists")
                                }
                            }

                    val mp = HashMap<String, Float>()
                    for (i in arrTimeToXAxis.indices) {
                        mp[arrTimeToXAxis[i]] = prefs.getFloat(arrTimeToXAxis[i], 40f)
                        if(previousMap[arrTimeToXAxis[i]]==null ||  previousMap[arrTimeToXAxis[i]] == 0f)
                            previousMap[arrTimeToXAxis[i]] = mp[arrTimeToXAxis[i]]!!
                        else
                            previousMap[arrTimeToXAxis[i]] = (mp[arrTimeToXAxis[i]]?:40f + previousMap[arrTimeToXAxis[i]]!!)/2
                    }
                    val calendar = Calendar.getInstance().time
                    val date = SimpleDateFormat("dd-MM-yyyy").format(calendar)
                    dbGlobalRoot.collection(mAuth.uid!!).document(date).set(mp)
                    dbGlobalRoot.collection(mAuth.uid!!).document("average").set(previousMap)
                    flag=true
                } else
                    Log.d("job", "error, can't log in")
            }
            delay(7000)
        }
        if(flag)
            return Result.success()
        return Result.failure()
    }
}