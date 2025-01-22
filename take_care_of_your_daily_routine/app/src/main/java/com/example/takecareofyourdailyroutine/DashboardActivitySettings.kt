package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.takecareofyourdailyroutine.databinding.ActivityDashboardSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class DashboardActivitySettings : AppCompatActivity() {
    lateinit var binding: ActivityDashboardSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTask.setOnClickListener {
            val intent = Intent(this, DashboardTaskActivity::class.java)
            startActivity(intent)
        }
        binding.btnHome.setOnClickListener {
            val intent = Intent(this, DashboardActivityHome::class.java)
            startActivity(intent)
        }
        binding.btnShop.setOnClickListener {
            val intent = Intent(this, DashboardShop::class.java)
            startActivity(intent)
        }


        // Obsługa przycisku wylogowania
        binding.btnLogout.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            auth.signOut() // Wylogowanie użytkownika z Firebase

            val sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear() // Usunięcie danych logowania
            editor.apply()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Zakończenie DashboardActivitySettings
        }
    }
}
