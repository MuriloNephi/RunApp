package com.ifpr.wearostemplate.presentation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifpr.wearostemplate.R

class PerfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContentView(R.layout.activity_perfil)

        val txtTotalCorridas = findViewById<TextView>(R.id.txtTotalCorridas)
        val txtDistanciaTotal = findViewById<TextView>(R.id.txtDistanciaTotal)

        val database = FirebaseDatabase.getInstance()
        val referencia = database.getReference("corridas")

        referencia.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalCorridas = 0
                var distanciaTotal = 0.0

                for (corrida in snapshot.children) {
                    totalCorridas++
                    val distancia = corrida.child("distanciaKm").getValue(Double::class.java) ?: 0.0
                    distanciaTotal += distancia
                }

                txtTotalCorridas.text = "Total de corridas: $totalCorridas"
                txtDistanciaTotal.text = "Distância total: %.2f km".format(distanciaTotal)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}