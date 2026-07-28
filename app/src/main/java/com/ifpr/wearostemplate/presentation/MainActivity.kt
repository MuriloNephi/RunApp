package com.ifpr.wearostemplate.presentation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var segundos = 0L
    private var correndo = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var txtTime: TextView

    private val cronometro = object : Runnable {
        override fun run() {
            if (correndo) {
                segundos++
                val minutos = segundos / 60
                val segs = segundos % 60
                txtTime.text = String.format("%02d:%02d", minutos, segs)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContentView(R.layout.activity_main)

        txtTime = findViewById(R.id.txtTime)

        val btnPerfil = findViewById<Button>(R.id.btnPerfil)
        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        btnPlay.setOnClickListener {
            if (!correndo) {
                correndo = true
                handler.post(cronometro)
            }
        }

        val btnStop = findViewById<Button>(R.id.btnStop)
        btnStop.setOnClickListener {
            if (correndo) {
                correndo = false
                val distanciaKm = 2.5
                salvarCorrida(distanciaKm, segundos)
                Toast.makeText(this, "Corrida salva!", Toast.LENGTH_SHORT).show()
                segundos = 0
                txtTime.text = "00:00"
            }
        }
    }

    private fun salvarCorrida(distanciaKm: Double, tempoSegundos: Long) {
        val database = FirebaseDatabase.getInstance()
        val referencia = database.getReference("corridas")
        val id = referencia.push().key ?: return
        val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val ritmo = calcularRitmo(distanciaKm, tempoSegundos)
        val corrida = Corrida(distanciaKm, tempoSegundos, ritmo, data)
        referencia.child(id).setValue(corrida)
    }

    private fun calcularRitmo(distanciaKm: Double, tempoSegundos: Long): String {
        if (distanciaKm <= 0.0) return "0:00"
        val segundosPorKm = (tempoSegundos / distanciaKm).toInt()
        val minutos = segundosPorKm / 60
        val segundos = segundosPorKm % 60
        return "$minutos:" + segundos.toString().padStart(2, '0')
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(cronometro)
    }
}