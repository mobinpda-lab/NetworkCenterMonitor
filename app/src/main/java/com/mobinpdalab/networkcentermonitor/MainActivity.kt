package com.mobinpdalab.networkcentermonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.mobinpdalab.networkcentermonitor.ui.NetworkCenterMonitorApp
import com.mobinpdalab.networkcentermonitor.ui.theme.NetworkCenterMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkCenterMonitorTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NetworkCenterMonitorApp()
                }
            }
        }
    }
}
