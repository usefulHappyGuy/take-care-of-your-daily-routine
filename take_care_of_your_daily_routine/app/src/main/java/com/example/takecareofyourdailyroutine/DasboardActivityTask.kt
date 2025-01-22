package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.takecareofyourdailyroutine.databinding.ActivityDashboardTaskBinding
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.google.firebase.firestore.FieldValue
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardTaskBinding
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskList: MutableList<Task>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicjalizacja RecyclerView
        binding.rvTaskList.layoutManager = LinearLayoutManager(this)
        taskList = mutableListOf()
        taskAdapter = TaskAdapter(
            taskList,
            onTaskClick = { task ->
                Toast.makeText(this, "Kliknięto na zadanie: ${task.title}", Toast.LENGTH_SHORT)
                    .show()
            },
            onTaskComplete = { task ->
                completeTask(task, taskAdapter, taskList)
            },
            onTaskDiscard = { task ->
                discardTask(task)
            }
        )
        binding.rvTaskList.adapter = taskAdapter

        // Pobieranie zadań z Firestore
        loadTasksFromFirestore()

        // Codzienne odświeżanie zadań
        checkAndRefreshTasks()

        // Obsługa przycisku dodawania nowego zadania
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog { task ->
                taskList.add(task)
                taskAdapter.notifyItemInserted(taskList.size - 1)
                addTaskToFirestore(task)
            }
        }

        // Przejście do innych aktywności
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, DashboardActivityHome::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, DashboardActivitySettings::class.java))
        }
        binding.btnShop.setOnClickListener {
            startActivity(Intent(this, DashboardShop::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun loadTasksFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val tasks =
                        documentSnapshot.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                    taskList.clear()

                    val existingTasksSet = mutableSetOf<Pair<String, String>>()


                    for (task in tasks) {
                        val id = task["id"]?.toString() ?: continue
                        val title = task["title"] as? String ?: continue
                        val description = task["description"] as? String ?: continue
                        val isCompleted = task["isCompleted"] as? Boolean ?: false
                        val date = task["date"] as? String ?: continue
                        val lastUpdated = task["lastUpdated"] as? String ?: continue
                        val recurring = task["recurring"] as? Boolean ?: false

                        val taskKey = title to description
                        if (!isCompleted && existingTasksSet.add(taskKey)) {
                            taskList.add(
                                Task(
                                    id,
                                    title,
                                    description,
                                    isCompleted,
                                    date,
                                    lastUpdated,
                                    recurring
                                )
                            )
                        }
                    }

                    taskAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Błąd podczas pobierania zadań", e)
                }
        }
    }

    private fun showAddTaskDialog(onTaskAdded: (Task) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val titleEditText: EditText = dialogView.findViewById(R.id.etTaskTitle)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.etTaskDescription)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Dodaj nowe zadanie")
            .setView(dialogView)
            .setPositiveButton("Dodaj") { _, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                if (title.isNotEmpty() && description.isNotEmpty()) {
                    val taskExists = taskList.any { it.title == title }
                    if (taskExists) {
                        Toast.makeText(
                            this,
                            "Zadanie o tej samej nazwie już istnieje!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val task = Task(
                            UUID.randomUUID().toString(),
                            title,
                            description,
                            false,
                            getCurrentDate(),
                            getCurrentDate()
                        )
                        onTaskAdded(task)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Proszę wprowadzić tytuł i opis zadania.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()
    }

    private fun addTaskToFirestore(task: Task) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { documentSnapshot ->
                    val tasks =
                        documentSnapshot.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                    if (!tasks.any { it["id"] == task.id }) {
                        db.collection("users").document(uid)
                            .update("tasks", FieldValue.arrayUnion(task.toMap()))
                            .addOnSuccessListener {
                                Log.d("Firestore", "Zadanie dodane.")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Błąd podczas dodawania zadania", e)
                            }
                    } else {
                        Log.d(
                            "Firestore",
                            "Zadanie z tym samym ID już istnieje, pominięto dodanie."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Błąd podczas sprawdzania istniejących zadań", e)
                }
        }
    }


    private fun completeTask(task: Task, adapter: TaskAdapter, taskList: MutableList<Task>) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Usuń zadanie lokalnie
        taskList.remove(task)
        adapter.notifyDataSetChanged()

        // Aktualizuj Firebase
        val taskRef = db.collection("users").document(uid)

        taskRef.get().addOnSuccessListener { documentSnapshot ->
            val tasks =
                documentSnapshot.get("tasks") as? MutableList<Map<String, Any>> ?: mutableListOf()

            val taskToRemove = tasks.find { it["id"] == task.id }
            if (taskToRemove != null) {
                tasks.remove(taskToRemove)
                taskRef.update("tasks", tasks)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Zadanie usunięte z `tasks`.")

                        val completedTaskMap = taskToRemove.toMutableMap().apply {
                            this["isCompleted"] = true
                            this["lastUpdated"] = getCurrentDate()
                            this["recurring"] = true // Zadanie staje się cykliczne po zakończeniu
                        }

                        taskRef.update("completedTasks", FieldValue.arrayUnion(completedTaskMap))
                            .addOnSuccessListener {
                                Log.d("Firestore", "Zadanie przeniesione do `completedTasks`.")

                                // Aktualizacja monet
                                val currentCoins = documentSnapshot.getLong("coins") ?: 0L
                                val updatedCoins = currentCoins + 5 // Dodajemy 5 monet za zakończenie zadania
                                taskRef.update("coins", updatedCoins)
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "Monety zaktualizowane. Aktualna ilość: $updatedCoins")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Błąd podczas aktualizacji monet", e)
                                    }

                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "Firestore",
                                    "Błąd podczas przenoszenia zadania do `completedTasks`",
                                    e
                                )
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Błąd podczas aktualizacji `tasks`", e)
                    }
            } else {
                Log.w("Firestore", "Nie znaleziono zadania o podanym ID w `tasks`.")
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Błąd podczas pobierania dokumentu użytkownika", e)
        }
    }


    private fun discardTask(task: Task) {
        removeTaskFromFirestore(task)
        val position = taskList.indexOf(task)
        if (position != -1) {
            taskList.removeAt(position)
            taskAdapter.notifyItemRemoved(position)
        }
    }

    private fun checkAndRefreshTasks() {
        val sharedPreferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE)
        val lastSyncDate = sharedPreferences.getString("lastSyncDate", null)
        val currentDate = getCurrentDate()

        if (lastSyncDate == null || lastSyncDate != currentDate) {
            resetDailyTasks()
            sharedPreferences.edit().putString("lastSyncDate", currentDate).apply()
        }
    }

    private fun resetDailyTasks() {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val completedTasks =
                        documentSnapshot.get("completedTasks") as? List<Map<String, Any>>
                            ?: emptyList()
                    val currentTasks =
                        documentSnapshot.get("tasks") as? List<Map<String, Any>> ?: emptyList()

                    val currentDate = getCurrentDate()

                    val tasksToReset = completedTasks.filter { taskMap ->
                        val isRecurring = taskMap["recurring"] as? Boolean ?: false
                        val lastUpdated = taskMap["lastUpdated"] as? String ?: ""
                        // Sprawdzamy, czy zadanie jest cykliczne i nie zostało przywrócone dzisiaj
                        isRecurring && lastUpdated != currentDate
                    }.map { taskMap ->
                        Task(
                            id = UUID.randomUUID().toString(), // Nowe ID dla zadania
                            title = taskMap["title"] as String,
                            description = taskMap["description"] as String,
                            isCompleted = false,
                            date = currentDate,
                            lastUpdated = currentDate,
                            recurring = false // Zadanie staje się nierekurencyjne
                        )
                    }

                    val existingTaskSet = currentTasks.map { it["title"] as String to it["description"]as String }.toSet()

                    val newTasks = tasksToReset.filterNot{
                        existingTaskSet.contains(it.title to it.description)}

                    if (newTasks.isNotEmpty()) {
                        db.collection("users").document(uid)
                            .update(
                                "tasks",
                                FieldValue.arrayUnion(*newTasks.map { it.toMap() }.toTypedArray())
                            )
                            .addOnSuccessListener {
                                Log.d("Firestore", "Cykliczne zadania zostały przywrócone.")
                                loadTasksFromFirestore() // Odśwież listę zadań lokalnie
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Błąd podczas przywracania cyklicznych zadań", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Błąd podczas pobierania ukończonych zadań", e)
                }
        }
    }


    private fun removeTaskFromFirestore(task: Task) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val tasks = documentSnapshot.get("tasks") as? MutableList<Map<String, Any>>
                    ?: mutableListOf()
                val updatedTasks = tasks.filterNot { it["id"] == task.id }

                db.collection("users").document(uid)
                    .update("tasks", updatedTasks)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Zadanie usunięte z `tasks`.")
                    }
            }
    }
}
