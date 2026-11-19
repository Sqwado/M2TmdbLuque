package com.example.m2tmdbluque

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m2tmdbluque.databinding.ActivitySensorDemoBinding
import java.util.Locale

class SensorDemoActivity : AppCompatActivity(), SensorEventListener, OnSensorItemClickListener {

    private lateinit var binding: ActivitySensorDemoBinding
    private lateinit var sensorListAdapter: SensorListAdapter
    private lateinit var sensorManager: SensorManager

    private var currentSensor: Sensor? = null
    private var selectedPosition = 0
    private var listening = false
    private var lastUiUpdateMs = 0L
    private val sensors: ArrayList<Sensor> = ArrayList()
    private val valuesBuffer = FloatArray(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.sensorListRv.setHasFixedSize(true)
        binding.sensorListRv.layoutManager = LinearLayoutManager(this)
        sensorListAdapter = SensorListAdapter(sensors, this)
        binding.sensorListRv.adapter = sensorListAdapter

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensors.addAll(sensorManager.getSensorList(Sensor.TYPE_ALL))
        sensorListAdapter.notifyItemRangeInserted(0, sensors.size)

        selectedPosition = savedInstanceState?.getInt(KEY_SELECTED_POSITION, 0) ?: 0
        if (sensors.isNotEmpty()) {
            val index = selectedPosition.coerceIn(0, sensors.lastIndex)
            selectSensor(index, registerIfResumed = false)
            binding.sensorListRv.scrollToPosition(index)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_POSITION, selectedPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onSensorItemClicked(position: Int) {
        Log.d(TAG, "onSensorItemClicked($position)")
        selectSensor(position, registerIfResumed = true)
    }

    private fun selectSensor(position: Int, registerIfResumed: Boolean) {
        if (position !in sensors.indices) return

        unRegisterSensorListener()
        selectedPosition = position
        sensorListAdapter.selectedPosition = position
        currentSensor = sensors[position]
        Log.i(TAG, "selected '${currentSensor?.name}'")

        binding.sensorDemoView.dialCaption = currentSensor?.name.orEmpty()
        binding.sensorDemoView.clearValues()
        binding.arrayValuesTv.text = getString(R.string.no_sensor_values)
        lastUiUpdateMs = 0L

        if (registerIfResumed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            registerSensorListener()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val count = minOf(event.values.size, valuesBuffer.size)
        System.arraycopy(event.values, 0, valuesBuffer, 0, count)
        val maxRange = event.sensor.maximumRange

        // Toujours lisser le cadran (copie des valeurs — SensorEvent est recyclé)
        binding.sensorDemoView.updateValues(valuesBuffer, count, maxRange)

        // Texte moins fréquent pour éviter de saturer le main thread
        val now = SystemClock.uptimeMillis()
        if (now - lastUiUpdateMs >= UI_TEXT_INTERVAL_MS) {
            lastUiUpdateMs = now
            binding.arrayValuesTv.text = formatValues(valuesBuffer, count, maxRange)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onAccuracyChanged($accuracy)")
    }

    private fun formatValues(values: FloatArray, count: Int, maxRange: Float): String = buildString {
        append("range = ").append(String.format(Locale.US, "%.3f", maxRange)).append('\n')
        for (i in 0 until count) {
            val label = COMPONENT_LABELS.getOrElse(i) { "v$i" }
            append(label).append("  =  ")
            append(String.format(Locale.US, "%+.4f", values[i]))
            if (i < count - 1) append('\n')
        }
    }

    private fun registerSensorListener() {
        val sensor = currentSensor ?: return
        if (listening) return
        val ok = sensorManager.registerListener(
            this,
            sensor,
            SAMPLING_PERIOD_US,
            MAX_REPORT_LATENCY_US,
        )
        listening = ok
        if (!ok) {
            Log.w(TAG, "registerListener failed for ${sensor.name}")
            binding.arrayValuesTv.text = getString(R.string.sensor_not_available)
        }
    }

    private fun unRegisterSensorListener() {
        if (!::sensorManager.isInitialized) return
        // Toujours détacher ce listener (évite les doubles inscriptions après rotation)
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onResume() {
        super.onResume()
        registerSensorListener()
    }

    override fun onPause() {
        unRegisterSensorListener()
        super.onPause()
    }

    override fun onDestroy() {
        unRegisterSensorListener()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SensorDemoActivity"

        /** ~20 Hz : fluide sans saturer le CPU (SENSOR_DELAY_UI ≈ 60 ms). */
        private const val SAMPLING_PERIOD_US = 50_000

        /** Batch matériel jusqu’à 100 ms → moins de wakeups, rendu toujours fluide via lerp. */
        private const val MAX_REPORT_LATENCY_US = 100_000

        private const val UI_TEXT_INTERVAL_MS = 50L
        private const val KEY_SELECTED_POSITION = "selected_sensor_position"

        private val COMPONENT_LABELS = arrayOf("X", "Y", "Z", "v3", "v4", "v5", "v6", "v7", "v8")
    }
}
