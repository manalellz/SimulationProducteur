package com.example.simulationproducteur


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var txtStock: TextView
    private lateinit var txtStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStart: Button

    private var stock = 0
    private val stockMax = 10

    private val lock = java.lang.Object()

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false

    private var producerThread: Thread? = null
    private var consumerThread: Thread? = null

    private var seconds = 0

    private val timer = object : Runnable {
        override fun run() {
            if (!running) return

            seconds++
            progressBar.progress = seconds

            if (seconds < 20) {
                handler.postDelayed(this, 1000)
            } else {
                stopSimulation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStock = findViewById(R.id.txtStock)
        txtStatus = findViewById(R.id.txtStatus)
        progressBar = findViewById(R.id.progressBar)
        btnStart = findViewById(R.id.btnStart)

        progressBar.max = 20
        progressBar.progress = 0

        btnStart.setOnClickListener {
            startSimulation()
        }
    }

    private fun startSimulation() {

        synchronized(lock) {
            stock = 0
            running = true
        }

        seconds = 0
        progressBar.progress = 0

        txtStock.text = "Stock actuel : 0"
        txtStatus.text = "Simulation en cours..."

        btnStart.isEnabled = false

        // Thread Producteur
        producerThread = Thread {

            try {
                while (running) {

                    synchronized(lock) {

                        while (running && stock >= stockMax) {
                            lock.wait()
                        }

                        if (!running) return@Thread

                        stock++

                        val valeur = stock

                        lock.notifyAll()

                        runOnUiThread {
                            txtStock.text =
                                "Stock actuel : $valeur"
                        }
                    }

                    Thread.sleep(500)
                }

            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // Thread Consommateur
        consumerThread = Thread {

            try {
                while (running) {

                    synchronized(lock) {

                        while (running && stock <= 0) {
                            lock.wait()
                        }

                        if (!running) return@Thread

                        stock--

                        val valeur = stock

                        lock.notifyAll()

                        runOnUiThread {
                            txtStock.text =
                                "Stock actuel : $valeur"
                        }
                    }

                    Thread.sleep(300)
                }

            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // Démarrer les deux threads
        producerThread?.start()
        consumerThread?.start()

        // Démarrer le compteur de 20 secondes
        handler.postDelayed(timer, 1000)
    }

    private fun stopSimulation() {

        synchronized(lock) {
            running = false
            lock.notifyAll()
        }

        producerThread?.interrupt()
        consumerThread?.interrupt()

        handler.removeCallbacks(timer)

        progressBar.progress = 20

        txtStatus.text = "Simulation terminée"

        btnStart.isEnabled = true
    }

    override fun onDestroy() {

        synchronized(lock) {
            running = false
            lock.notifyAll()
        }

        producerThread?.interrupt()
        consumerThread?.interrupt()

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}
