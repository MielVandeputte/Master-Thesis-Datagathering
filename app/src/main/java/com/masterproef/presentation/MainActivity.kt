package com.masterproef.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.masterproef.model.Model
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : ComponentActivity() {

    companion object {
        // Are the sensors active or not
        private val _sensorsActiveState = MutableLiveData(false)
        private val sensorsActiveState: LiveData<Boolean> = _sensorsActiveState

        // Are the sensors done or not, can the switch to the next activity be made
        private val _sensorsDoneState = MutableLiveData(false)
        private val sensorsDoneState: LiveData<Boolean> = _sensorsDoneState

        // Current userid
        private val _userState = MutableLiveData(1)
        private val userState: LiveData<Int> = _userState

        // The current recordingsession index
        private var recordId = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp(sensorsActiveState, sensorsDoneState, userState, changeUser = ::changeUser, switchActivities = ::switchActivities) }

        // Create the model singleton and pass the context of the activity into it
        Model.init(this)

        // Get the saved userid so the user doesn't have to set it every time the activity is startedf
        val sharedPreference: SharedPreferences = getSharedPreferences("data", MODE_PRIVATE)
        _userState.value = sharedPreference.getInt("userid", 1)

        _sensorsActiveState.postValue(false)
        _sensorsDoneState.postValue(false)

        activateSensors()
    }

    // Cancel the timers and deactivate the sensors
    override fun onDestroy() {
        super.onDestroy()

        val firstIntent = Intent(this, MyBroadCastReceiver::class.java)
        val secondIntent = PendingIntent.getBroadcast(this, 0, firstIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(secondIntent)

        Model.stopListening()
    }

    private fun activateSensors() {
        var timeMinutes: Long = 2

        // Determines if the activity follows an activity of type EvalActivity or if the activity was created together with the app
        if (intent.getBooleanExtra("subsequentMeasurement", false)) {
            timeMinutes = 20
        }

        // If the activity follows another activity, wait for 20 minutes and make the broadcastreceiver start the sensors
        // If the activity was started together with the app, wait for 2 minutes and make the broadcastreceiver start the sensors
        val firstIntent = Intent(this, MyBroadCastReceiver::class.java)
        val secondIntent = PendingIntent.getBroadcast(this, 0, firstIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION_CODES.S > Build.VERSION.SDK_INT || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeMinutes * 60 * 1000, secondIntent)
        } else {
            Toast.makeText(this, "Timer couldn't be set, device not supported", Toast.LENGTH_SHORT).show()
        }
    }

    // Function is called with value 1 if the '+'-button is pressed to increase the userId
    // Function is called with value -1 if the '-'-button is pressed to decrease the userId
    private fun changeUser(change: Int) {
        val currentUser: Int? = _userState.value

        // If the new value is positive, set it and save it to storage
        if (currentUser != null && currentUser + change > 0) {
            _userState.value = currentUser + change

            val sharedPreference = getSharedPreferences("data", MODE_PRIVATE)
            val editor = sharedPreference.edit()
            editor.putInt("userid", _userState.value!!)
            editor.apply()
        }
    }

    // Ends the current activity and switches to a new activity of type 'EvalActivity'
    private fun switchActivities() {
        startActivity(Intent(this, EvalActivity::class.java))
        finish()
    }

    // Receives intent when the set timer finishes
    class MyBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            // Sensorstate is set to active so the user can't change his/her userid anymore
            _sensorsActiveState.postValue(true)

            // Record id is increased and set in storage
            val sharedPreference = context?.getSharedPreferences("data", MODE_PRIVATE)
            sharedPreference?.edit()?.putInt("recordid", recordId+1)?.apply()
            recordId += 1

            // All sensors are activated and start filling their cache
            Model.startListening()

            // After 2 minutes, the sensors are deactivated again and the wearable vibrates to let the user know
            Timer().schedule(2 * 60 * 1000) {
                if (Model.isListening()) {
                    val vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                }

                Model.stopListening()

                // The cache of all sensors is emptied to the CSV files and the sensordonestate is set to true
                // so the userid remains unchangeable and the user can switch to the other activity
                if (_userState.value != null) {
                    Model.flushToFile(_userState.value!!, recordId)
                    _sensorsDoneState.postValue(true)
                }

                _sensorsActiveState.postValue(false)
            }
        }
    }
}

@Composable
fun WearApp(sensorsActiveValue: LiveData<Boolean>, sensorsDoneValue: LiveData<Boolean>, userId: LiveData<Int>, changeUser: (Int) -> Unit, switchActivities: () -> Unit) {
    val observedSensorsActiveValue by sensorsActiveValue.observeAsState()
    val observedSensorsDoneValue by sensorsDoneValue.observeAsState()
    val observedUserId by userId.observeAsState()

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Huidige gebruiker: $observedUserId")
        Text(text = "Sensors actief: $observedSensorsActiveValue")

        Button(onClick = { changeUser(1) }, enabled = !observedSensorsDoneValue!! && !observedSensorsActiveValue!!) {
            Text(text = "Volgende gebruiker", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(16.dp))
        }

        Button(onClick = { changeUser(-1) }, enabled = !observedSensorsDoneValue!! && !observedSensorsActiveValue!!) {
            Text(text = "Vorige gebruiker", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(16.dp))
        }

        Button(onClick = { switchActivities() }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red), enabled = observedSensorsDoneValue!!) {
            Text(text = "Evalueer Meting", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}