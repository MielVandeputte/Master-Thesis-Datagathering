package com.masterproef.model

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat

// Singleton requests permissions, holds LoggableSensor objects and distributes a call to the singleton to all LoggableSensor objects
// so all sensors can be controlled at once
object Model{

    private lateinit var sensorList: List<LoggableSensor>
    private var isListening: Boolean = false

    fun init(context: Activity){
        ActivityCompat.requestPermissions(
            context, arrayOf(
                // Protection level: normal
                Manifest.permission.FOREGROUND_SERVICE,
                // Protection level: dangerous
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), 0
        )

        sensorList = listOf(
            LoggableSensor(65547,"ppSensor", wakeUp = true, triggerSensor = false, context = context),
            LoggableSensor(31,"heartBeatSensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(21,"heartRateSensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(5,"lightSensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(65544,"ppgGainSensor", wakeUp = true, triggerSensor = false, context = context),
            LoggableSensor(65541,"ppgSensor", wakeUp = true, triggerSensor = false, context = context),
            LoggableSensor(17,"sigMotionSensor", wakeUp = true, triggerSensor = true, context = context),
            LoggableSensor(4,"gyroscopeSensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(9,"gravitySensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(10,"linAccelerationSensor", wakeUp = false, triggerSensor = false, context = context),
            LoggableSensor(15,"gameRotationSensor", wakeUp = false, triggerSensor = false, context = context),
        )
    }

    // Activates all sensors so they fill their cache
    fun startListening(){
        isListening = true
        sensorList.forEach{ s -> s.startListening()}
    }

    fun stopListening(){
        isListening = false
        sensorList.forEach{ s -> s.stopListening()}
    }

    fun isListening(): Boolean {
        return isListening
    }

    // Flush the cache of all sensors to their respective CSV file, together with the
    // userid (as determined by the buttons in the UI) and the recordid (index of the recordingsession) in each row
    fun flushToFile(user: Int, record: Int){
        sensorList.forEach{ s -> s.flushToFile(user, record)}
    }
}