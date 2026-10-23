package com.somkov.greek_b1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.somkov.greek_b1.theme.GreekB1Theme

private const val TAG = "GreekB1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity created")
        enableEdgeToEdge()
        setContent {
            GreekB1Theme {
                AppNavigation()
            }
        }
    }
}
