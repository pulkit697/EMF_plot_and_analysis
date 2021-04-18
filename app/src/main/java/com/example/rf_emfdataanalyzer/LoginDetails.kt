package com.example.rf_emfdataanalyzer

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

class LoginDetails {
    companion object
    {
        @Volatile
        var LOGIN_CREDENTIAL:String?=null

        fun getLoginCredential(context: Context):String
        {
            return ""
        }
    }
}