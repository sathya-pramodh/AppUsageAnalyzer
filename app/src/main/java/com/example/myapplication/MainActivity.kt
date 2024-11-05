@file:Suppress("NAME_SHADOWING")

package com.example.myapplication

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var db: FirebaseFirestore
    private var sequenceId: Long = -1
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        if (hasUsageStatsPermission()) {
            displayUsageStats()
        } else {
            requestUsageStatsPermission()
        }
        db = FirebaseFirestore.getInstance()
        val devId = Settings.Secure.getString(
           contentResolver,
           Settings.Secure.ANDROID_ID
        )
        GlobalScope.launch {
            val sequences = db.collection("sequences").get().await().documents
            for (sequence in sequences) {
                val seqId = sequence.get("seq-id")
                val seqName = sequence.get("seq-name")
                if (seqName == devId) {
                    sequenceId = seqId as Long
                    break
                }
            }
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
   private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun displayUsageStats() {
        val currentTime = System.currentTimeMillis()

        // Query usage stats for the last day
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 1000 * 60 * 60 * 24,
            currentTime
        )

        if (usageStatsList.isNotEmpty()) {
            println(usageStatsList)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

            for (usageStats in usageStatsList) {
                val appName = usageStats.packageName
                val currentTime = dateFormat.format(Date(System.currentTimeMillis()))
                if (sequenceId == -1L) {
                    print("Seq ID not found")
                    continue
                }
                val appLog = AppLog(appName, sequenceId, currentTime)
                db.collection("logs").add(appLog)
            }
        } else {
            println("No usage data available")
        }
    } 
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}