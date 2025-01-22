package com.example.takecareofyourdailyroutine

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.takecareofyourdailyroutine.databinding.ActivityDashboardShopBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardShop : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pobierz przekazaną liczbę monet
        val userCoins = intent.getIntExtra("userCoins", 0)
        binding.tvUserCoins.text = "Twoje monety: $userCoins"

        // Obsługa przycisków nawigacyjnych
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, DashboardActivityHome::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, DashboardActivitySettings::class.java))
        }
        binding.btnTask.setOnClickListener {
            startActivity(Intent(this, DashboardTaskActivity::class.java))
        }

        // Elementy sklepu
        val shopItems = listOf(
            ShopItem("Zamrożenie streaka", 50, R.drawable.freeze)
        )

        val adapter = ShopAdapter(shopItems) { selectedItem ->
            if (userCoins >= selectedItem.price) {
                purchaseItem(selectedItem.price) {
                    if (selectedItem.name == "Zamrożenie streaka") {
                        freezeStreak(
                            onSuccess = {
                                Toast.makeText(this, "Zamrożono streak!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                Toast.makeText(this, "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Nie masz wystarczających monet na zakup ${selectedItem.name}!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.recyclerViewShop.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewShop.adapter = adapter
    }

    private fun purchaseItem(itemPrice: Int, onSuccess: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val currentCoins = document.getLong("coins") ?: 0
                if (currentCoins >= itemPrice) {
                    // Odejmij monety i zwiększ licznik zamrożeń
                    db.collection("users").document(uid)
                        .update(
                            mapOf(
                                "coins" to FieldValue.increment(-itemPrice.toLong()),
                                "freezeCount" to FieldValue.increment(1)
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Zakup udany! Zamrożenie dodane.", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Błąd podczas zakupu.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Nie masz wystarczających monet.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Błąd podczas sprawdzania monet.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun freezeStreak(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val frozenDate = document.getString("frozenDate")
                    if (frozenDate == currentDate) {
                        onFailure(Exception("Streak został już zamrożony dzisiaj!"))
                    } else {
                        db.collection("users").document(uid)
                            .update("frozenDate", currentDate)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                } else {
                    onFailure(Exception("Nie znaleziono danych użytkownika!"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
