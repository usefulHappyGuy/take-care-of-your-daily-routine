package com.example.takecareofyourdailyroutine

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DateDetailsActivity : AppCompatActivity() {

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var habitList: MutableList<Habit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_date_details)

        val selectedDate = intent.getStringExtra("SELECTED_DATE") ?: return
        val tvDate = findViewById<TextView>(R.id.tvDate)
        tvDate.text = "Nawyki z dnia: $selectedDate"

        val rvHabits = findViewById<RecyclerView>(R.id.rvHabits)
        rvHabits.layoutManager = LinearLayoutManager(this)

        habitList = mutableListOf()
        habitAdapter = HabitAdapter(habitList)
        rvHabits.adapter = habitAdapter

        loadHabitsForDate(selectedDate)
    }

    private fun loadHabitsForDate(date: String) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("completedHabits")
            .document(date)
            .get()
            .addOnSuccessListener { document ->
                val habits = document.get("habits") as? List<Map<String, Any>> ?: emptyList()
                habitList.clear()
                for (habit in habits) {
                    val title = habit["title"] as? String ?: continue
                    val description = habit["description"] as? String ?: ""
                    habitList.add(Habit(title, description))
                }
                habitAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                // Obsługa błędu
                e.printStackTrace()
            }
    }
}
