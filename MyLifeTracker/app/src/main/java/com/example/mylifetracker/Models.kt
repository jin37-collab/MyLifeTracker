package com.example.mylifetracker

import java.time.LocalDate
import java.time.LocalTime

enum class TaskCategory(val label: String) { PERSONAL("개인"), STUDY("공부"), WORK("업무"), HEALTH("건강") }
enum class ExpenseCategory(val label: String) { FOOD("식사"), CAFE("카페"), DATE("데이트"), CLOTHES("의류"), GOODS("물건") }

data class TaskItem(
    val id: Long,
    val title: String,
    val category: TaskCategory,
    val dueDate: LocalDate? = null,
    val completed: Boolean = false
)

data class ScheduleItem(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val place: String = ""
)

data class ExpenseItem(
    val id: Long,
    val amount: Int,
    val category: ExpenseCategory,
    val cardId: Int,
    val memo: String,
    val date: LocalDate
)

data class IncomeItem(
    val id: Long,
    val amount: Int,
    val memo: String,
    val date: LocalDate
)

data class CardProfile(
    val id: Int,
    val name: String,
    val monthlyGoal: Int
)
