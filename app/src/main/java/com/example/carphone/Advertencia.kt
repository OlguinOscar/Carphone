package com.example.carphone

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Advertencia : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_advertencia)
        val botonAd: Button = findViewById(R.id.buttonSigA)
        botonAd.setOnClickListener {
            val intent = Intent(this, Permisos::class.java)
            startActivity(intent)
        }

    }
}