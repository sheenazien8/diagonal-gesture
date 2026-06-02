package com.sheenadev.diagonalgesture

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sheenadev.diagonalgesture.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: GesturePreferenceManager

    private var isServiceRunning = false

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (AppPicker.canDrawOverlays(this)) {
            checkNotificationPermissionAndStart()
        } else {
            updateServiceButtonState(false)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startGestureService()
        } else {
            updateServiceButtonState(false)
        }
    }

    private val appPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val packageName = result.data?.getStringExtra(ActivityPickerActivity.EXTRA_PACKAGE_NAME) ?: return@registerForActivityResult
            val appName = result.data?.getStringExtra(ActivityPickerActivity.EXTRA_APP_NAME) ?: ""
            val activityName = result.data?.getStringExtra(ActivityPickerActivity.EXTRA_ACTIVITY_NAME) ?: ""
            val activityLabel = result.data?.getStringExtra(ActivityPickerActivity.EXTRA_ACTIVITY_LABEL) ?: ""
            
            prefsManager.saveTargetApp(packageName, appName, activityName, activityLabel)
            updateAppSelectionUI(appName, activityLabel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = GesturePreferenceManager(this)
        setupUI()
        updateUIFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        updateServiceButtonState(GestureOverlayServiceHelper.isServiceRunning(this))
        updateOverlayPermissionStatus()
    }

    private fun setupUI() {
        binding.btnServiceToggle.setOnClickListener {
            toggleService()
        }

        binding.switchDebug.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.debugMode = isChecked
            updateDebugModeUI(isChecked)
            if (isServiceRunning) {
                GestureOverlayService.updateSettings(this)
            }
        }

        updateDebugModeUI(prefsManager.debugMode)

        binding.btnSelectApp.setOnClickListener {
            if (!AppPicker.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            appPickerLauncher.launch(Intent(this, ActivityPickerActivity::class.java))
        }

        binding.btnClearTarget.setOnClickListener {
            prefsManager.clearTargetApp()
            updateAppSelectionUI("")
        }

        setupPositionSpinner()
        setupSizeSeekBars()
    }

    private fun setupPositionSpinner() {
        val positions = arrayOf("Bottom Right", "Bottom Left", "Both Corners")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPosition.adapter = adapter

        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefsManager.triggerPosition = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSizeSeekBars() {
        binding.seekbarWidth.progress = prefsManager.areaWidthDp.toInt()
        binding.seekbarHeight.progress = prefsManager.areaHeightDp.toInt()

        binding.seekbarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefsManager.areaWidthDp = progress.toFloat()
                    updateSizeLabels()
                    if (isServiceRunning) {
                        GestureOverlayService.updateSettings(this@MainActivity)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekbarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefsManager.areaHeightDp = progress.toFloat()
                    updateSizeLabels()
                    if (isServiceRunning) {
                        GestureOverlayService.updateSettings(this@MainActivity)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefsManager.triggerPosition = position
                if (isServiceRunning) {
                    GestureOverlayService.updateSettings(this@MainActivity)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.seekbarThreshold.progress = prefsManager.swipeThreshold

        binding.seekbarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minThreshold = 30
                    prefsManager.swipeThreshold = maxOf(progress, minThreshold)
                    updateThresholdLabel()
                    if (isServiceRunning) {
                        GestureOverlayService.updateSettings(this@MainActivity)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateSizeLabels()
        updateThresholdLabel()
    }

    private fun updateThresholdLabel() {
        binding.tvThresholdValue.text = "${prefsManager.swipeThreshold} px"
    }

    private fun updateSizeLabels() {
        binding.tvWidthValue.text = "${prefsManager.areaWidthDp.toInt()} dp"
        binding.tvHeightValue.text = "${prefsManager.areaHeightDp.toInt()} dp"
    }

    private fun updateUIFromPrefs() {
        binding.spinnerPosition.setSelection(prefsManager.triggerPosition)
        binding.seekbarWidth.progress = prefsManager.areaWidthDp.toInt()
        binding.seekbarHeight.progress = prefsManager.areaHeightDp.toInt()
        binding.switchDebug.isChecked = prefsManager.debugMode
        binding.seekbarThreshold.progress = prefsManager.swipeThreshold
        updateSizeLabels()
        updateThresholdLabel()

        if (prefsManager.hasTargetApp()) {
            val activityLabel = prefsManager.targetActivityLabel
            updateAppSelectionUI(prefsManager.targetAppName, activityLabel)
        } else {
            updateAppSelectionUI("")
        }
    }

    private fun updateAppSelectionUI(appName: String, activityLabel: String = "") {
        val displayText = if (activityLabel.isNotEmpty()) {
            "$appName → $activityLabel"
        } else if (appName.isNotEmpty()) {
            appName
        } else {
            "No app selected"
        }
        
        if (appName.isEmpty()) {
            binding.tvSelectedApp.text = displayText
            binding.btnClearTarget.visibility = View.GONE
        } else {
            binding.tvSelectedApp.text = displayText
            binding.btnClearTarget.visibility = View.VISIBLE
        }
    }

    private fun updateOverlayPermissionStatus() {
        val hasPermission = AppPicker.canDrawOverlays(this)
        if (hasPermission) {
            binding.tvOverlayStatus.text = "Overlay permission: Granted"
            binding.tvOverlayStatus.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else {
            binding.tvOverlayStatus.text = "Overlay permission: Required"
            binding.tvOverlayStatus.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
        }
    }

    private fun toggleService() {
        if (!AppPicker.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        if (!prefsManager.hasTargetApp()) {
            android.widget.Toast.makeText(
                this,
                "Please select a target app first",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isServiceRunning) {
            GestureOverlayService.stopService(this)
            prefsManager.isServiceEnabled = false
            updateServiceButtonState(false)
        } else {
            checkNotificationPermissionAndStart()
        }
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startGestureService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startGestureService()
        }
    }

    private fun startGestureService() {
        prefsManager.isServiceEnabled = true
        GestureOverlayService.startService(this)
        updateServiceButtonState(true)
    }

    private fun updateDebugModeUI(isDebugEnabled: Boolean) {
        val visibility = if (isDebugEnabled) View.VISIBLE else View.GONE 
        binding.tvHelpText.visibility = visibility
        binding.tvLogCommand.visibility = visibility
    }

    private fun updateServiceButtonState(running: Boolean) {
        isServiceRunning = running
        if (running) {
            binding.btnServiceToggle.text = "Stop Service"
            binding.btnServiceToggle.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.holo_red_light)
            )
        } else {
            binding.btnServiceToggle.text = "Start Service"
            binding.btnServiceToggle.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
}

object GestureOverlayServiceHelper {
    fun isServiceRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.className == GestureOverlayService::class.java.name) {
                return true
            }
        }
        return false
    }
}
