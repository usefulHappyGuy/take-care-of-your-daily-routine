package com.example.takecareofyourdailyroutine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.takecareofyourdailyroutine.R

class ShopAdapter(
    private var items: List<ShopItem>,
    private val onBuyClick: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    // ViewHolder
    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivItemIcon)
        val name: TextView = itemView.findViewById(R.id.tvItemName)
        val price: TextView = itemView.findViewById(R.id.tvItemPrice)
        val buyButton: Button = itemView.findViewById(R.id.btnBuy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = items[position]

        // Ustaw dane w widoku
        holder.icon.setImageResource(item.iconResId)
        holder.name.text = item.name
        holder.price.text = "Cena: ${item.price}"

        // Obsługa kliknięcia przycisku "Kup"
        holder.buyButton.setOnClickListener {
            onBuyClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    // Metoda do aktualizacji listy elementów
    fun updateItems(newItems: List<ShopItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
