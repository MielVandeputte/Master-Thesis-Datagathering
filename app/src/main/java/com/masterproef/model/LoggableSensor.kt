package com.masterproef.model

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.*
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileWriter

// Controls a specific sensor for which the type is passed in
class LoggableSensor(private val sensorType: Int, private val typeName: String, wakeUp: Boolean, private val triggerSensor: Boolean, context: Context) : SensorEventListener, TriggerEventListener() {

    private var sensorManager: SensorManager
    private var sensor: Sensor

    private val cache: MutableList<SensorRecord> = mutableListOf()

    // Holds the start and endtime of the current recordingsession
    private var startTime: Long = 0
    private var endTime: Long = 0

    init {
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(sensorType, wakeUp)
    }

    /* Clears the cache and activates the sensors
     * Some sensors are of type triggersensor and some are normal sensors as passed in in the class declaration
     * Triggersensors don't have values but just get triggered when something occurs
     * These require different function calls to activate
     * Timestamp is set at which this happens
     * */
    fun startListening() {
        cache.clear()
        startTime = SystemClock.elapsedRealtimeNanos()

        if (triggerSensor) {
            sensorManager.requestTriggerSensor(this, sensor)
        } else {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    // Deactivates the sensors and sets the timestamp at which this happenes
    fun stopListening() {
        if (triggerSensor) {
            sensorManager.cancelTriggerSensor(this, sensor)
        } else {
            sensorManager.unregisterListener(this)
        }

        endTime = SystemClock.elapsedRealtimeNanos()
    }

    /* Is called when any normal sensor is active and has a new value
     * Checks if it's the sensor, represented by this class
     * If so, create a SensorRecord object and add it to the cache
     * */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == sensorType) {
            cache.add(SensorRecord(SystemClock.elapsedRealtimeNanos(), event.values.copyOf()))
        }
    }

    /* Is called when any triggersensor is active and has a new value
     * Checks if it's the sensor, represented by this class
     * If so, create a SensorRecord object and add it to the cache
     * */
    override fun onTrigger(event: TriggerEvent?) {
        sensorManager.requestTriggerSensor(this, sensor)
        cache.add(SensorRecord(SystemClock.elapsedRealtimeNanos(), FloatArray(0)))
    }

    // Saves the cache to a particular CSV file,as passed in in the constructor of the class
    fun flushToFile(userId: Int, recordId: Int) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$typeName.csv")
        val fileWriter = FileWriter(file, true)

        for (record in cache) {
            var dataString: String = record.timestamp.toString() + ", " + userId.toString() + ", " + recordId.toString()

            for (value in record.values) {
                dataString += (", $value")
            }

            fileWriter.append(dataString + "\n")
        }

        fileWriter.close()
        cache.clear()

        Log.i("Loggable Sensor", "Sensordata of $typeName flushed to file")

        // Saves information about the recordingsession to a particular CSV file
        val metaFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sensorInformation.csv")
        val metaFileWriter = FileWriter(metaFile, true)
        metaFileWriter.append(sensorType.toString() + ", " + userId.toString() + ", " + recordId.toString() + ", " + startTime.toString() + ", " + endTime.toString() + ", " + SystemClock.elapsedRealtimeNanos().toString() + ", "  + System.currentTimeMillis().toString() + "\n")
        metaFileWriter.close()
    }

    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {
        return
    }
}