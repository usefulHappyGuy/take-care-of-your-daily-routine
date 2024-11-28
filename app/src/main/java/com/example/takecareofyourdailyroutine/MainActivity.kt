package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.takecareofyourdailyroutine.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Sprawdzenie, czy użytkownik jest już zalogowany
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Użytkownik jest zalogowany, przejdź od razu do głównej aktywności
            navigateToDashboard()
        }
        */
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Funkcja do przejścia do ekranu głównego, np. dashboardu
    private fun navigateToDashboard() {
        //  przekierowywanie użytkownika do głównej części aplikacji
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()  // Zakończenie MainActivity, aby nie wrócić do niej
    }
}
