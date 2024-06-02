package com.example.carphone

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
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
import android.content.IntentFilter
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
   // private val deviceAddress = "00:22:12:02:5B:38"
    private val deviceAddress = "98:DA:20:06:7F:86"
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var Connected = false
    private lateinit var bluetoothStateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_huella_bt)
        setupAuth()

        Thread{
            setupBluetoothStateReceiver()
        }.start()


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
            }
            else{
                Toast.makeText(this, "Ya estás conectado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBluetoothStateReceiver() {
        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device?.address == deviceAddress) {
                        handleBluetoothDisconnect()
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    private fun handleBluetoothDisconnect() {
        bluetoothSocket?.close()
        outputStream?.close()
        Connected = false
        runOnUiThread {
            Toast.makeText(this, "Dispositivo Bluetooth desconectado", Toast.LENGTH_SHORT).show()
            findViewById<ImageView>(R.id.conectado).visibility = View.INVISIBLE
            findViewById<ImageView>(R.id.noconectado).visibility = View.VISIBLE
            findViewById<TextView>(R.id.sitext).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.notexto).visibility = View.VISIBLE
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
        canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (canAuthenticate) {
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biometrica")
                .setSubtitle("Autentíquese utilizando el sensor")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
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

    private val btActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth activado, intenta conectarte de nuevo", Toast.LENGTH_LONG).show()
        }
    }

    private fun activateBluetooth(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btActivityResultLauncher.launch(enableIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun ConectarBT() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            activateBluetooth(bluetoothAdapter)
        }
        else{
            Thread {
                try {
                    val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
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

    override fun onBackPressed() {
        if (Connected) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }


}