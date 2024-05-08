package com.example.carphone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView.FindListener
import android.widget.Button


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!(isUserRegistered())){
            setContentView(R.layout.activity_main)
            val BotonSiguiente: Button = findViewById(R.id.Siguiente)
            BotonSiguiente.setOnClickListener {
                val intent = Intent (this,Advertencia::class.java)
                startActivity(intent)
            }
        }
        else{
            setContentView(R.layout.activity_main)
            val BotonSiguiente: Button = findViewById(R.id.Siguiente)
            BotonSiguiente.setOnClickListener {
                val intent = Intent(this, HuellaBT::class.java)
                startActivity(intent)
            }
        }


    }

    private fun isUserRegistered(): Boolean {
        // Obtener las SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPreferences", 0)
        // Retornar el valor guardado, false como valor predeterminado
        return sharedPref.getBoolean("isRegistered", false)
    }
}