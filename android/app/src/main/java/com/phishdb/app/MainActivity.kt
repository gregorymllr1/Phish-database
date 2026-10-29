package com.phishdb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phishdb.app.ui.PhishRoot
import com.phishdb.app.ui.theme.PhishDBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PhishApp).container
        setContent {
            PhishDBTheme {
                PhishRoot(container = container)
            }
        }
    }
}
