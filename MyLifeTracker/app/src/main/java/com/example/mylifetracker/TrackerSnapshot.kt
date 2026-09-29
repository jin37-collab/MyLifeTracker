package com.example.mylifetracker

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

data class TrackerSnapshot(
    val tasks: List<TaskItem>,
    val schedules: List<ScheduleItem>,
    val expenses: List<ExpenseItem>,
    val incomes: List<IncomeItem>,
    val cards: List<CardProfile>,
    val incomeGoal: Int,
    val updatedAt: Long
) {
    fun hasUserData(): Boolean = tasks.isNotEmpty() || schedules.isNotEmpty() || expenses.isNotEmpty() || incomes.isNotEmpty()
}

object TrackerSnapshotJson {
    fun encode(snapshot: TrackerSnapshot): String = JSONObject().apply {
        put("schemaVersion", 2)
        put("updatedAt", snapshot.updatedAt)
        put("incomeGoal", snapshot.incomeGoal)
        put("tasks", JSONArray().apply {
            snapshot.tasks.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("category", item.category.name)
                    put("dueDate", item.dueDate?.toString() ?: JSONObject.NULL)
                    put("completed", item.completed)
                })
            }
        })
        put("schedules", JSONArray().apply {
            snapshot.schedules.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date", item.date.toString())
                    put("time", item.time.toString())
                    put("place", item.place)
                })
            }
        })
        put("expenses", JSONArray().apply {
            snapshot.expenses.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("amount", item.amount)
                    put("category", item.category.name)
                    put("cardId", item.cardId)
                    put("memo", item.memo)
                    put("date", item.date.toString())
                })
            }
        })
        put("incomes", JSONArray().apply {
            snapshot.incomes.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("amount", item.amount)
                    put("memo", item.memo)
                    put("date", item.date.toString())
                })
            }
        })
        put("cards", JSONArray().apply {
            snapshot.cards.forEach { card ->
                put(JSONObject().apply {
                    put("id", card.id)
                    put("name", card.name)
                    put("goal", card.monthlyGoal)
                })
            }
        })
    }.toString()

    fun decode(raw: String): TrackerSnapshot? = runCatching {
        if (raw.isBlank() || raw.trim() == "null") return null
        val root = JSONObject(raw)
        val tasks = root.optJSONArray("tasks").toTaskList()
        val schedules = root.optJSONArray("schedules").toScheduleList()
        val expenses = root.optJSONArray("expenses").toExpenseList()
        val incomes = root.optJSONArray("incomes").toIncomeList()
        val cards = root.optJSONArray("cards").toCardList().ifEmpty { TrackerStore.defaultCards() }
        TrackerSnapshot(
            tasks = tasks,
            schedules = schedules,
            expenses = expenses,
            incomes = incomes,
            cards = cards,
            incomeGoal = root.optInt("incomeGoal", 1_000_000),
            updatedAt = root.optLong("updatedAt", 0L)
        )
    }.getOrNull()

    private fun JSONArray?.toTaskList(): List<TaskItem> = buildList {
        val arr = this@toTaskList ?: return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(TaskItem(
                id = o.getLong("id"),
                title = o.getString("title"),
                category = TaskCategory.valueOf(o.getString("category")),
                dueDate = o.optString("dueDate").takeIf { it.isNotBlank() && it != "null" }?.let(LocalDate::parse),
                completed = o.optBoolean("completed", false)
            ))
        }
    }

    private fun JSONArray?.toScheduleList(): List<ScheduleItem> = buildList {
        val arr = this@toScheduleList ?: return@buildList
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

    private fun JSONArray?.toExpenseList(): List<ExpenseItem> = buildList {
        val arr = this@toExpenseList ?: return@buildList
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

    private fun JSONArray?.toIncomeList(): List<IncomeItem> = buildList {
        val arr = this@toIncomeList ?: return@buildList
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

    private fun JSONArray?.toCardList(): List<CardProfile> = buildList {
        val arr = this@toCardList ?: return@buildList
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(CardProfile(o.getInt("id"), o.getString("name"), o.getInt("goal")))
        }
    }
}
