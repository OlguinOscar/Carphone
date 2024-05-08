package com.example.carphone

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible


class HuellaBT : AppCompatActivity() {
    // HUELLA DIGITAL
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var canAuthenticate = false
    private var outputStream: OutputStream? = null
    // BT
    private var bluetoothSocket: BluetoothSocket? = null
   // private val deviceAddress = "00:21:13:00:24:A4"
    private val deviceAddress = "98:DA:20:06:7F:86"//CAMBIAR
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var PermisoBT = false
    private var Connected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_huella_bt)
        setupAuth()

        val BotonBT : ImageButton = findViewById(R.id.BT)
        val BotonHuella : ImageButton = findViewById(R.id.Huella)

        val tache: ImageView = findViewById(R.id.noconectado)
        val circulo: ImageView =findViewById(R.id.conectado)

        tache.visibility = ImageView.VISIBLE
        circulo.visibility = ImageView.INVISIBLE

        val conectado : TextView =findViewById(R.id.sitext)
        val noconectado: TextView = findViewById(R.id.notexto)

        conectado.visibility = TextView.INVISIBLE
        noconectado.visibility = TextView.VISIBLE

        BotonHuella.setOnClickListener{
            if(Connected){
                authenticate { authenticated ->
                    if (authenticated) {
                        Toast.makeText(this, "¡Autenticación exitosa! Auto Encendido", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "¡Autenticación fallida o cancelada!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                Toast.makeText(this, "Tienes que conectarte primero.", Toast.LENGTH_SHORT).show()
            }
        }
        BotonBT.setOnClickListener{
            if(!Connected){
                ConectarBT()
                EscanearBT()
            }
            else{
                Toast.makeText(this, "Ya estás conectado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun authenticate(auth: (Boolean) -> Unit) {
        if (canAuthenticate) {
            val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    auth(true)
                    EnviarDato("c")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    auth(false)
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
    private fun setupAuth() {
        val biometricManager = BiometricManager.from(this)
        canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        if (canAuthenticate) {
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biometrica")
                .setSubtitle("Autentíquese utilizando el sensor")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        }
    }
    private fun EnviarDato(data: String) {
        try {
            outputStream?.write(data.toByteArray())
            Toast.makeText(this, "Datos enviados exitosamente.", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error al enviar datos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //Atributos de BT
    private fun EscanearBT() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no soportado", Toast.LENGTH_LONG).show()
            return
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                PermisoBT = true
                activateBluetooth(bluetoothAdapter)
            }
        }
    }
    private val bluetoothPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        PermisoBT = isGranted
        if (isGranted) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            activateBluetooth(bluetoothAdapter)
        }
    }

    private val btActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            EscanearBT()
        }
    }

    private fun activateBluetooth(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btActivityResultLauncher.launch(enableIntent)
        } else {
            EscanearBT()
        }
    }

    @SuppressLint("MissingPermission")
    private fun ConectarBT() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth no disponible o no activado", Toast.LENGTH_LONG).show()
            return
        }
        val device: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(deviceAddress)
        Thread {
            try {
                val socket = device?.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                bluetoothSocket = socket
                outputStream = bluetoothSocket?.outputStream

                runOnUiThread {
                    Toast.makeText(this, "Conexión exitosa", Toast.LENGTH_SHORT).show()
                    Connected = true
                    findViewById<ImageView>(R.id.conectado).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.noconectado).visibility = View.INVISIBLE

                    findViewById<TextView>(R.id.sitext).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.notexto).visibility = View.INVISIBLE

                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
                    findViewById<ImageView>(R.id.conectado).visibility = View.INVISIBLE
                    findViewById<ImageView>(R.id.noconectado).visibility = View.VISIBLE

                    findViewById<TextView>(R.id.sitext).visibility = View.INVISIBLE
                    findViewById<TextView>(R.id.notexto).visibility = View.VISIBLE
                }
            }
        }.start()
    }

}