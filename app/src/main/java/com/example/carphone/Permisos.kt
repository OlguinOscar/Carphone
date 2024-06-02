package com.example.carphone

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

        private var BTPermiso = false //Permiso de Bluetooth
        private var canAuthenticate = false
        lateinit var promptInfo: androidx.biometric.BiometricPrompt.PromptInfo
        var HuellaPermiso = false
class Permisos : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permisos)

        setupAuth()
        val botonBluetoothPermisos: Button = findViewById(R.id.BTPermiso)
        val botonHuellaPermiso: Button = findViewById(R.id.HuellaPermiso)
        val botonSiguienteP: Button = findViewById(R.id.SiguientePermiso)

        botonBluetoothPermisos.setOnClickListener {
            if(BTPermiso){
                Toast.makeText(this, "Permiso ya autenticado, pasa a otra opción.", Toast.LENGTH_SHORT).show()
            }
            else{
                EscanearBT()
            }
        }
        botonHuellaPermiso.setOnClickListener {
            if (HuellaPermiso){
                Toast.makeText(this, "¡Ya te autenticaste!", Toast.LENGTH_SHORT).show()
            }
            else{
                Autenticacion { authenticated ->
                    if (authenticated) {
                        Toast.makeText(this, "¡Autenticación exitosa!", Toast.LENGTH_SHORT).show()
                        HuellaPermiso = true
                    } else {
                        Toast.makeText(this, "¡Autenticación fallida o cancelada!", Toast.LENGTH_SHORT).show()
                    }
                }
           }
        }
        botonSiguienteP.setOnClickListener {
            if(BTPermiso && HuellaPermiso){
                val sharedPref = getSharedPreferences("MyAppPreferences", 0)
                with (sharedPref.edit()) {
                    putBoolean("isRegistered", true)
                    apply()
                }
                val intent = Intent(this, HuellaBT::class.java)
                startActivity(intent)
            }
            else{
                Toast.makeText(this, "Por favor verifica las opciones anteriores.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    /// PERMISOS DE BLUETOOTH
    private fun EscanearBT() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG).show()
            return
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            else {
                BTPermiso = true
                activateBluetooth(bluetoothAdapter)
            }
        }
    }

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            BTPermiso = isGranted
            if (isGranted) {
                val bluetoothManager =
                    getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
                activateBluetooth(bluetoothAdapter)
            }
        }

    private fun activateBluetooth(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btActivityResultLauncher.launch(enableIntent)
        }
    }

    private val btActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth activado", Toast.LENGTH_LONG).show()
            }
        }
    ///// PERMISOS DE BLUETOOTH

    //// EMPIEZAN PERMISOS DE HUELLA

    fun setupAuth() {
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        canAuthenticate = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        if (canAuthenticate) {
            promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biometrica")
                .setSubtitle("Autentíquese utilizando el sensor")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
        }
    }

   fun Autenticacion (auth: (Boolean) -> Unit) {
        if (canAuthenticate) {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor, object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    auth(false)
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    auth(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    auth(false)
                }
            })
            biometricPrompt.authenticate(promptInfo)
        } else {
            auth(false)
        }
   }

}