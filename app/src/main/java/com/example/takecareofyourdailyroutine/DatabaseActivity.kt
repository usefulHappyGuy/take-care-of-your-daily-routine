    package com.example.takecareofyourdailyroutine


    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import java.security.MessageDigest
    import android.util.Log

    // Funkcja do hashowania hasła (SHA-256)
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

        fun saveUserToFirestore(email: String, name: String, password: String) {
        val db = FirebaseFirestore.getInstance()

        val hashedPassword = hashPassword(password)
        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "password" to hashedPassword
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener {
                    Log.d("Firestore", "Użytkownik zapisany w bazie danych!")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Błąd podczas zapisywania użytkownika", e)
                }
        } else {
            Log.e("Firestore", "Nie udało się pobrać UID użytkownika!")
        }
    }