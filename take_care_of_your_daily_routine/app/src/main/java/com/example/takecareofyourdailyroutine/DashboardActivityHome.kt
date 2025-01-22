package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.takecareofyourdailyroutine.databinding.ActivityDashboardHomeBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class DashboardActivityHome : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardHomeBinding
    private lateinit var habitAdapter: HabitAdapter
    private var selectedDate: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the habit adapter
        habitAdapter = HabitAdapter(emptyList())

        // Set up RecyclerView
        binding.rvCompletedTasks.layoutManager = LinearLayoutManager(this)
        binding.rvCompletedTasks.adapter = habitAdapter

        // Load tasks for the current date
        loadTasksForDate(selectedDate)

        updateDateHeader(selectedDate)

        updateCoinsUI()

        // Button listeners
        binding.btnTask.setOnClickListener {
            startActivity(Intent(this, DashboardTaskActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, DashboardActivitySettings::class.java))
        }
        binding.btnShop.setOnClickListener {
            fetchUserCoins { coins ->
                val intent = Intent(this, DashboardShop::class.java)
                intent.putExtra("userCoins", coins)
                startActivity(intent)
            }
        }

        binding.btnOpenCalendar.setOnClickListener {
            openDatePicker()
        }

    }

    private fun openDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedTimestamp ->
            // Konwersja wybranej daty na sformatowaną datę
            val selectedDate = Date(selectedTimestamp)
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)

            // Zaktualizuj nagłówek z datą
            updateDateHeader(formattedDate)

            // Załaduj zadania dla wybranej daty
            loadTasksForDate(formattedDate)
        }

        datePicker.show(supportFragmentManager, "MaterialDatePicker")
    }

    private fun updateDateHeader(date: String) {
        binding.tvTaskTitle.text = "Nawyki z dnia: $date"
    }

    private fun loadTasksForDate(selectedDate: String) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    // Pobierz listę ukończonych zadań
                    val completedTasks =
                        documentSnapshot.get("completedTasks") as? List<Map<String, Any>>
                            ?: emptyList()

                    // Filtrowanie ukończonych zadań dla wybranej daty
                    val filteredTasks = completedTasks.filter { task ->
                        task["date"] == selectedDate
                    }.map { task ->
                        Habit(
                            title = task["title"].toString(),
                            description = task["description"].toString()
                        )
                    }

                    // Zaktualizuj adapter z ukończonymi zadaniami
                    habitAdapter.updateHabits(filteredTasks)

                    // Pobierz freezeCount z Firestore
                    fetchFreezeCount { freezeCount ->
                        // Oblicz streak i zaktualizuj UI
                        val streak = calculateStreak(completedTasks, freezeCount)
                        updateStreakUI(streak)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Błąd podczas ładowania ukończonych zadań", e)
                }
        }
    }

    private fun calculateStreak(completedTasks: List<Map<String, Any>>, freezeCount: Int): Int {
        Log.d("Streak", "Rozpoczynamy obliczanie passy")
        val streakThreshold = 3 // Minimalna liczba ukończonych zadań w jednym dniu
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Grupowanie zadań po dacie
        Log.d("Streak", "Dane przed grupowaniem: $completedTasks")
        val groupedTasks = completedTasks.groupBy { it["date"] as? String ?: "" }
        Log.d("Streak", "Dane po grupowaniu: $groupedTasks")

        val sortedDates = groupedTasks.keys
            .filter { it.isNotEmpty() }
            .map { dateFormat.parse(it)!! }
            .sorted()

        // Liczenie streaka
        var streak = 0
        var previousDate: Date? = null
        var remainingFreezeCount = freezeCount // Licznik dostępnych zamrożeń

        for (date in sortedDates) {
            Log.d("Streak", "Przetwarzanie daty: ${dateFormat.format(date)}")
            if (previousDate == null || isNextDay(previousDate, date)) {
                val taskCount = groupedTasks[dateFormat.format(date)]?.size ?: 0
                Log.d("Streak", "Liczba zadań w dniu ${dateFormat.format(date)}: $taskCount")
                if (taskCount >= streakThreshold) {
                    streak++
                } else if (remainingFreezeCount > 0) {
                    // Automatyczne zamrożenie streaka
                    streak++
                    remainingFreezeCount--
                    updateFreezeCount(remainingFreezeCount)
                } else {
                    break
                }
            } else if (!isSameDay(previousDate, date)) {
                break
            }
            previousDate = date
        }
        Log.d("Streak", "Obliczona passa: $streak")
        return streak
    }

    private fun isNextDay(previous: Date, current: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar1.time = previous
        calendar1.add(Calendar.DATE, 1)
        calendar2.time = current

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }


    private fun isSameDay(previous: Date, current: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar1.time = previous
        calendar2.time = current
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateStreakUI(streak: Int) {
        binding.tvStreak.text = "Twoja passa wynosi: $streak"
    }

    fun fetchUserCoins(onResult: (Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val coins = document.getLong("coins")?.toInt() ?: 0
                    onResult(coins)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Błąd podczas pobierania liczby monet", e)
            }
    }

    private fun updateCoinsUI() {
        fetchUserCoins { coins ->
            binding.tvCoins.text = "Monety: $coins"
        }
    }

    private fun updateFreezeCount(newCount: Int) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update("freezeCount", newCount)
            .addOnSuccessListener {
                Log.d("Firestore", "Zaktualizowano licznik zamrożeń: $newCount")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Błąd podczas aktualizacji licznika zamrożeń", e)
            }
    }

    private fun fetchFreezeCount(onResult: (Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val freezeCount = document.getLong("freezeCount")?.toInt() ?: 0
                    onResult(freezeCount)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Błąd podczas pobierania liczby zamrożeń", e)
            }
    }
}



