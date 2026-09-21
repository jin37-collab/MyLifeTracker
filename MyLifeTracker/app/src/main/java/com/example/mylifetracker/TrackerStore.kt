package com.example.mylifetracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class TrackerStore(context: Context) {
    private val prefs = context.getSharedPreferences("tracker_store", Context.MODE_PRIVATE)

    fun saveTasks(items: List<TaskItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("category", item.category.name)
                put("dueDate", item.dueDate?.toString() ?: JSONObject.NULL)
                put("completed", item.completed)
            })
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    fun loadTasks(): List<TaskItem> = runCatching {
        val raw = prefs.getString("tasks", null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(TaskItem(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    category = TaskCategory.valueOf(o.getString("category")),
                    dueDate = o.optString("dueDate").takeIf { it.isNotBlank() && it != "null" }?.let(LocalDate::parse),
                    completed = o.getBoolean("completed")
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveSchedules(items: List<ScheduleItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("date", item.date.toString())
                put("time", item.time.toString())
                put("place", item.place)
            })
        }
        prefs.edit().putString("schedules", arr.toString()).apply()
    }

    fun loadSchedules(): List<ScheduleItem> = runCatching {
        val raw = prefs.getString("schedules", null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ScheduleItem(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    date = LocalDate.parse(o.getString("date")),
                    time = LocalTime.parse(o.getString("time")),
                    place = o.optString("place")
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveExpenses(items: List<ExpenseItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("amount", item.amount)
                put("category", item.category.name)
                put("cardId", item.cardId)
                put("memo", item.memo)
                put("date", item.date.toString())
            })
        }
        prefs.edit().putString("expenses", arr.toString()).apply()
    }

    fun loadExpenses(): List<ExpenseItem> = runCatching {
        val raw = prefs.getString("expenses", null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(ExpenseItem(
                    id = o.getLong("id"),
                    amount = o.getInt("amount"),
                    category = ExpenseCategory.valueOf(o.getString("category")),
                    cardId = o.getInt("cardId"),
                    memo = o.optString("memo"),
                    date = LocalDate.parse(o.getString("date"))
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveIncomes(items: List<IncomeItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("amount", item.amount)
                put("memo", item.memo)
                put("date", item.date.toString())
            })
        }
        prefs.edit().putString("incomes", arr.toString()).apply()
    }

    fun loadIncomes(): List<IncomeItem> = runCatching {
        val raw = prefs.getString("incomes", null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(IncomeItem(
                    id = o.getLong("id"),
                    amount = o.getInt("amount"),
                    memo = o.optString("memo"),
                    date = LocalDate.parse(o.getString("date"))
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveIncomeGoal(value: Int) = prefs.edit().putInt("incomeGoal", value).apply()
    fun isInitialized(): Boolean = prefs.getBoolean("initialized", false)
    fun markInitialized() = prefs.edit().putBoolean("initialized", true).apply()
    fun loadIncomeGoal(): Int = prefs.getInt("incomeGoal", 1_000_000)

    fun saveCards(cards: List<CardProfile>) {
        val arr = JSONArray()
        cards.forEach { card ->
            arr.put(JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("goal", card.monthlyGoal)
            })
        }
        prefs.edit().putString("cards", arr.toString()).apply()
    }

    fun loadCards(): List<CardProfile> = runCatching {
        val raw = prefs.getString("cards", null) ?: return defaultCards()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(CardProfile(o.getInt("id"), o.getString("name"), o.getInt("goal")))
            }
        }
    }.getOrElse { defaultCards() }

    companion object {
        fun defaultCards() = listOf(
            CardProfile(1, "우리카드", 500_000),
            CardProfile(2, "신한카드", 300_000),
            CardProfile(3, "현대카드", 700_000)
        )
    }
}
