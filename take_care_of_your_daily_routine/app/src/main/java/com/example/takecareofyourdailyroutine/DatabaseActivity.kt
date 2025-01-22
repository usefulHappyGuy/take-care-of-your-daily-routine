package com.example.takecareofyourdailyroutine

import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.security.MessageDigest
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date


// Funkcja do hashowania hasła (SHA-256)
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Zapisywanie użytkownika do Firestore
fun saveUserToFirestore(email: String, name: String, password: String) {
    val db = FirebaseFirestore.getInstance()
    val hashedPassword = hashPassword(password)

    val userDocument = hashMapOf(
        "email" to email,
        "name" to name,
        "password" to hashedPassword,
        "tasks" to emptyList<Map<String, Any>>(),
        "routineTasks" to emptyList<Map<String, Any>>(),
        "coins" to 0 // Dodanie pola coins z wartością początkową 0
    )

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid != null) {
        db.collection("users").document(uid)
            .set(userDocument)
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

// Model danych dla Task
@Parcelize
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    var isCompleted: Boolean = false,
    val date: String,
    var lastUpdated: String,
    var recurring: Boolean = false // Domyślnie zadanie nie jest cykliczne
) : Parcelable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "isCompleted" to isCompleted,
            "date" to date,
            "lastUpdated" to lastUpdated,
            "recurring" to recurring
        )
    }
}
data class Habit(
    val title: String,
    val description: String
)

data class ShopItem(
    val name: String,
    val price: Int,
    val iconResId: Int // ID zasobu drawable
)


// Dodawanie Task do Firestore
fun addTaskToFirestore(task: Task, localTaskList: MutableList<Task>, adapter: TaskAdapter) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid != null) {
        task.lastUpdated = System.currentTimeMillis().toString()

        db.collection("users").document(uid)
            .update("tasks", FieldValue.arrayUnion(task.toMap()))
            .addOnSuccessListener {
                Log.d("Firestore", "Zadanie dodane do bazy danych!")
                localTaskList.add(task)
                adapter.addTask(task)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Błąd podczas dodawania zadania", e)
            }
    } else {
        Log.e("Firestore", "Nie udało się pobrać UID użytkownika!")
        // Informacja dla użytkownika
    }
}

// Nasłuchiwanie zmian w Firestore
fun listenForTasks(adapter: TaskAdapter, localTaskList: MutableList<Task>) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    db.collection("users").document(uid)
        .addSnapshotListener { document, error ->
            if (error != null) {
                Log.e("Firestore", "Błąd nasłuchiwania zmian", error)
                return@addSnapshotListener
            }
            if (document != null && document.exists()) {
                val tasks = document.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                val taskList = tasks.map { taskMap ->
                    Task(
                        id = taskMap["id"] as String,
                        title = taskMap["title"] as String,
                        description = taskMap["description"] as String,
                        isCompleted = taskMap["isCompleted"] as Boolean,
                        date = taskMap["date"] as String,
                        lastUpdated = taskMap["lastUpdated"] as? String ?: ""
                    )
                }
                localTaskList.clear()
                localTaskList.addAll(taskList)
                adapter.refreshTasks(taskList)
            }
        }
}

fun updateUserCoins(coinsToAdd: Int) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    db.collection("users").document(uid)
        .update("coins", FieldValue.increment(coinsToAdd.toLong()))
        .addOnSuccessListener {
            Log.d("Firestore", "Monety użytkownika zaktualizowane o $coinsToAdd!")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Błąd podczas aktualizacji monet użytkownika", e)
        }
}




