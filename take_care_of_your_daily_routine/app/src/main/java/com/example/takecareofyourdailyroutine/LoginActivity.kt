package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.takecareofyourdailyroutine.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()  // Instancja FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.editTextUsername.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Podaj adres e-mail i hasło", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }
    }

    // Funkcja logowania użytkownika
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jeśli logowanie zakończone sukcesem, przejście do głównej aktywności
                    Toast.makeText(this, "Logowanie udane", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivityHome::class.java)
                    startActivity(intent)
                    finish()  // Zakończenie tej aktywności, żeby użytkownik nie mógł wrócić
                } else {
                    // Błąd logowania, wyświetlenie komunikatu
                    Toast.makeText(this, "Błąd logowania: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
