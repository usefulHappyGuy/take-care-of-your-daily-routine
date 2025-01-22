package com.example.takecareofyourdailyroutine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskComplete: (Task) -> Unit,
    private val onTaskDiscard: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val taskStatus: TextView = itemView.findViewById(R.id.tvTaskStatus)
        val btnCompleteTask: Button = itemView.findViewById(R.id.btnCompleteTask)
        val btnDiscardTask: Button = itemView.findViewById(R.id.btnDiscardTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.taskTitle.text = task.title
        holder.taskStatus.text = if (task.isCompleted) "Zakończone" else "W toku"

        // Obsługa kliknięcia całego elementu
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }

        // Obsługa przycisku "Zakończ"
        holder.btnCompleteTask.setOnClickListener {
            if (!task.isCompleted) {
                onTaskComplete(task) // Wywołaj callback do aktualizacji globalnej
            }
        }

        // Obsługa przycisku "Porzuć"
        holder.btnDiscardTask.setOnClickListener {
            onTaskDiscard(task) // Wywołaj callback do obsługi usunięcia
        }
    }

    override fun getItemCount(): Int = taskList.size

    // Funkcja do odświeżenia całej listy
    fun refreshTasks(newTaskList: List<Task>) {
        taskList.clear()
        taskList.addAll(newTaskList)
        notifyDataSetChanged() // Odśwież całą listę
    }

    // Funkcja do dodawania zadania
    fun addTask(task: Task) {
        taskList.add(task)
        notifyItemInserted(taskList.size - 1) // Odśwież widok po dodaniu elementu
    }

    // Funkcja do usuwania zadania
    fun removeTask(task: Task) {
        val position = taskList.indexOfFirst { it.id == task.id }
        if (position != -1) {
            taskList.removeAt(position)
            notifyItemRemoved(position) // Odśwież widok po usunięciu elementu
        }
    }
}


