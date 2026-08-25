package com.ifpr.wearostemplate.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase
import com.ifpr.wearostemplate.R
import com.ifpr.wearostemplate.presentation.baseclasses.Corrida
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var segundos = 0L
    private var correndo = false
    private var distanciaKm = 0.0
    private var ultimaLocalizacao: Location? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var txtTime: TextView
    private lateinit var txtDistancia: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val pedirPermissao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            iniciarCorrida()
        } else {
            Toast.makeText(this, "Permissão de localização negada!", Toast.LENGTH_SHORT).show()
        }
    }

    private val cronometro = object : Runnable {
        override fun run() {
            if (correndo) {
                segundos++
                val minutos = segundos / 60
                val segs = segundos % 60
                txtTime.text = String.format(Locale.getDefault(), "%02d:%02d", minutos, segs)
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
        txtDistancia = findViewById(R.id.txtDistancia)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (localizacao in result.locations) {
                    ultimaLocalizacao?.let {
                        val distancia = it.distanceTo(localizacao) / 1000.0
                        distanciaKm += distancia
                        txtDistancia.text = String.format(Locale.getDefault(), "%.2f", distanciaKm)
                    }
                    ultimaLocalizacao = localizacao
                }
            }
        }

        val btnPerfil = findViewById<Button>(R.id.btnPerfil)
        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        btnPlay.setOnClickListener {
            if (!correndo) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    iniciarCorrida()
                } else {
                    pedirPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        val btnStop = findViewById<Button>(R.id.btnStop)
        btnStop.setOnClickListener {
            if (correndo) {
                correndo = false
                pararLocalizacao()
                salvarCorrida(distanciaKm, segundos)
                Toast.makeText(this, "Corrida salva!", Toast.LENGTH_SHORT).show()
                segundos = 0
                distanciaKm = 0.0
                ultimaLocalizacao = null
                txtTime.text = "00:00"
                txtDistancia.text = "0.00"
            }
        }
    }

    private fun iniciarCorrida() {
        correndo = true
        distanciaKm = 0.0
        ultimaLocalizacao = null
        handler.post(cronometro)
        iniciarLocalizacao()
    }

    private fun iniciarLocalizacao() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun pararLocalizacao() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
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
        pararLocalizacao()
    }
}