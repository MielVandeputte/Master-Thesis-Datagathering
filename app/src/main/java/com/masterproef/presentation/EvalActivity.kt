package com.masterproef.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import java.io.File
import java.io.FileWriter

class EvalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp(submit = ::saveAndReturn) }
    }

    // Gets called when the save button is pressed at the bottom of the UI
    private fun saveAndReturn(watching: Boolean, rating: Int, fatigue: Int) {

        // User and record id is taken from storage
        val sharedPreference = getSharedPreferences("data", Context.MODE_PRIVATE)
        val userId = sharedPreference.getInt("userid",-1)
        val recordId = sharedPreference.getInt("recordid",-1)

        // Answers are written to a CSV file together with the user and record id
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "evalAnswers.csv")
        val fileWriter = FileWriter(file, true)
        fileWriter.append("${SystemClock.elapsedRealtimeNanos()}, $userId, $recordId, $watching, $rating, $fatigue\n")
        fileWriter.close()

        // Switch back to a new instance of MainActivity
        // Set a flag in the intent to indicate that this instance is started from an instance of EvalActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("subsequentMeasurement", true)

        startActivity(intent)
        finish()
    }
}

@Composable
fun WearApp(submit: (Boolean, Int, Int) -> Unit) {

        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            var watching by remember { mutableStateOf(false) }
            var rating by remember { mutableStateOf(0) }
            var fatigue by remember { mutableStateOf(0) }

            Text(text = "TV aan het kijken?")
            Text(text = "Huidige waarde: $watching")

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = { watching = true }) {
                    Text(text = "Ja", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }

                Button(onClick = { watching = false }) {
                    Text(text = "Nee", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }
            }

            Text(text = "Hoe goed/slechtgezind?")
            Text(text = "Huidige waarde: $rating / 4")

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = { if (rating > 0) { rating-- } }) {
                    Text(text = "-", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }

                Button(onClick = { if (rating < 4) { rating++ } },) {
                    Text(text = "+", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }
            }

            Text(text = "Hoe moe?")
            Text(text = "Huidige waarde: $fatigue / 4")

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = { if (fatigue > 0) { fatigue-- } },) {
                    Text(text = "-", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }

                Button(onClick = { if (fatigue < 4) { fatigue++ } },) {
                    Text(text = "+", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),)
                }
            }

            Button(onClick = { submit(watching, rating, fatigue) }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),) {
                Text(text = "Sla Op", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(16.dp),)
            }
    }
}