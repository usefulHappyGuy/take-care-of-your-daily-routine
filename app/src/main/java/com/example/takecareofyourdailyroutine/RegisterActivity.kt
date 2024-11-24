package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var btnRegister: Button
    private lateinit var loginLink: TextView

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()  // FirebaseAuth instancja

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameEditText = findViewById(R.id.Name)
        emailEditText = findViewById(R.id.Email)
        passwordEditText = findViewById(R.id.Password)
        btnRegister = findViewById(R.id.btnRegister)
        loginLink = findViewById(R.id.LoginLink)

        btnRegister.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Walidacja danych
            if (validateInputs(name, email, password)) {
                // Rejestracja użytkownika w Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Rejestracja udana!", Toast.LENGTH_SHORT).show()
                            // Po udanej rejestracji, przejście do MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()  // Zakończenie bieżącej aktywności
                        } else {
                            Toast.makeText(this, "Błąd rejestracji: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Funkcja walidacji
    private fun validateInputs(name: String, email: String, password: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            nameEditText.error = "Imię jest wymagane"
            return false
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Adres e-mail jest wymagany"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Podaj poprawny adres e-mail"
            return false
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.error = "Hasło jest wymagane"
            return false
        }

        if (password.length < 6) {
            passwordEditText.error = "Hasło musi mieć co najmniej 6 znaków"
            return false
        }

        return true
    }
}
