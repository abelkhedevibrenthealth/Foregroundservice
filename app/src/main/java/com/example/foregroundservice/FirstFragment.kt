package com.example.foregroundservice

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foregroundservice.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    val healthConnectManager by lazy {
        HealthConnectManager(requireContext())
    }
    var sessionsList: MutableLiveData<List<ExerciseSession>> = MutableLiveData(listOf())
        private set

    var permissionsGranted = MutableLiveData(false)
        private set

    var uiState: UiState = UiState.Uninitialized
        private set

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    // Create the permissions launcher.
    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    private val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(permissions)) {
                // Permissions successfully granted
            } else {
                // Lack of required permissions
            }
        }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    var  ACTION_STOP_FOREGROUND = "${context?.packageName}.stopforeground"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        ACTION_STOP_FOREGROUND = "${context?.packageName}.stopforeground"
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.btnStart.setOnClickListener {
            context?.startForegroundService(Intent(context, SampleForegroundService::class.java))
            updateTextStatus()
        }
        binding.btnStop.setOnClickListener {
            val intentStop = Intent(context, SampleForegroundService::class.java)
            intentStop.action = ACTION_STOP_FOREGROUND
            context?.startForegroundService(intentStop)
            Handler().postDelayed({
                updateTextStatus()
            },100)
        }

        binding.checkHCIsAvailable.setOnClickListener {
            checkAvailability()
        }

        binding.checkPermission.setOnClickListener {
            lifecycleScope.launch {
                checkPermissions()
            }
        }

        binding.getPermission.setOnClickListener {
            lifecycleScope.launch {
                checkPermissionsAndRun()
            }
        }

        binding.revokePermission.setOnClickListener {
            lifecycleScope.launch {
                healthConnectManager.revokeAllPermissions()
            }
        }

        updateTextStatus()
    }

    private suspend fun checkPermissions() {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)

        if(permissionsGranted.value == true) {
            binding.checkPermissionMsg.text = "permissions are granted"
        } else {
            binding.checkPermissionMsg.text = "permissions are not granted"
        }
    }

    private fun updateTextStatus() {
        if(isMyServiceRunning(SampleForegroundService::class.java)){
            binding.txtServiceStatus?.text = "Service is Running"
        }else{
            binding.txtServiceStatus?.text = "Service is NOT Running"
        }
    }

    private fun checkAvailability() {
        healthConnectManager.checkAvailability()
        if(healthConnectManager.availability == HealthConnectAvailability.INSTALLED ) {
            binding.healthConnectMsg.text = "Health connect app is installed"
        } else {
            binding.healthConnectMsg.text = "Health connect app is not installed"
        }
    }


    suspend fun checkPermissionsAndRun() {
        val granted = healthConnectManager.hasAllPermissions(permissions)
        if (granted) {
            binding.getPermissionMsg.text = "permissions are granted"
            // Permissions already granted, proceed with inserting or reading data.
        } else {
            requestPermissions.launch(permissions)
        }
    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        try {

            val manager =
                context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(
                Int.MAX_VALUE
            )) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}