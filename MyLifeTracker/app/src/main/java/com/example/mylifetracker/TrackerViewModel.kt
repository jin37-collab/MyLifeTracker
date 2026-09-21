package com.example.mylifetracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.LocalTime

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TrackerStore(application)

    val tasks = mutableStateListOf<TaskItem>()
    val schedules = mutableStateListOf<ScheduleItem>()
    val expenses = mutableStateListOf<ExpenseItem>()
    val incomes = mutableStateListOf<IncomeItem>()
    val cards = mutableStateListOf<CardProfile>()
    var incomeGoal by mutableIntStateOf(store.loadIncomeGoal())
        private set

    init {
        tasks.addAll(store.loadTasks())
        schedules.addAll(store.loadSchedules())
        expenses.addAll(store.loadExpenses())
        incomes.addAll(store.loadIncomes())
        cards.addAll(store.loadCards())
        if (cards.isEmpty()) cards.addAll(TrackerStore.defaultCards())
        if (!store.isInitialized()) {
            seedDemoData()
            store.markInitialized()
        }
    }

    private fun id(): Long = System.currentTimeMillis() * 10 + (0..9).random()

    fun addTask(title: String, category: TaskCategory, dueDate: LocalDate?) {
        if (title.isBlank()) return
        tasks += TaskItem(id(), title.trim(), category, dueDate)
        store.saveTasks(tasks)
    }

    fun toggleTask(taskId: Long) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) tasks[index] = tasks[index].copy(completed = !tasks[index].completed)
        store.saveTasks(tasks)
    }

    fun deleteTask(taskId: Long) {
        tasks.removeAll { it.id == taskId }
        store.saveTasks(tasks)
    }

    fun addSchedule(title: String, date: LocalDate, time: LocalTime, place: String) {
        if (title.isBlank()) return
        schedules += ScheduleItem(id(), title.trim(), date, time, place.trim())
        store.saveSchedules(schedules)
    }

    fun deleteSchedule(itemId: Long) {
        schedules.removeAll { it.id == itemId }
        store.saveSchedules(schedules)
    }

    fun addExpense(amount: Int, category: ExpenseCategory, cardId: Int, memo: String) {
        if (amount <= 0) return
        expenses += ExpenseItem(id(), amount, category, cardId, memo.trim(), LocalDate.now())
        store.saveExpenses(expenses)
    }

    fun deleteExpense(itemId: Long) {
        expenses.removeAll { it.id == itemId }
        store.saveExpenses(expenses)
    }

    fun addIncome(amount: Int, memo: String) {
        if (amount <= 0) return
        incomes += IncomeItem(id(), amount, memo.trim(), LocalDate.now())
        store.saveIncomes(incomes)
    }

    fun deleteIncome(itemId: Long) {
        incomes.removeAll { it.id == itemId }
        store.saveIncomes(incomes)
    }

    fun updateIncomeGoal(value: Int) {
        if (value <= 0) return
        incomeGoal = value
        store.saveIncomeGoal(value)
    }

    fun updateCard(card: CardProfile) {
        val index = cards.indexOfFirst { it.id == card.id }
        if (index >= 0) cards[index] = card
        store.saveCards(cards)
    }

    fun cardSpent(cardId: Int, month: LocalDate = LocalDate.now()): Int = expenses
        .filter { it.cardId == cardId && it.date.year == month.year && it.date.month == month.month }
        .sumOf { it.amount }

    fun monthExpense(month: LocalDate = LocalDate.now()): Int = expenses
        .filter { it.date.year == month.year && it.date.month == month.month }
        .sumOf { it.amount }

    fun monthIncome(month: LocalDate = LocalDate.now()): Int = incomes
        .filter { it.date.year == month.year && it.date.month == month.month }
        .sumOf { it.amount }

    private fun seedDemoData() {
        val today = LocalDate.now()
        tasks += listOf(
            TaskItem(id(), "영어 단어 30개 외우기", TaskCategory.STUDY, today),
            TaskItem(id(), "프로젝트 기획안 제출", TaskCategory.WORK, today.plusDays(1)),
            TaskItem(id(), "병원 예약하기", TaskCategory.PERSONAL, today.plusDays(3)),
            TaskItem(id(), "방 청소하기", TaskCategory.PERSONAL),
            TaskItem(id(), "읽고 싶은 책 정리", TaskCategory.PERSONAL),
            TaskItem(id(), "운동 루틴 만들기", TaskCategory.HEALTH)
        )
        schedules += listOf(
            ScheduleItem(id(), "아침 운동", today, LocalTime.of(8, 0), "헬스장"),
            ScheduleItem(id(), "팀 미팅", today, LocalTime.of(10, 0), "회의실 A"),
            ScheduleItem(id(), "점심 약속", today, LocalTime.of(12, 0), "강남"),
            ScheduleItem(id(), "프로젝트 작업", today, LocalTime.of(14, 0), "카페"),
            ScheduleItem(id(), "영화 보기", today, LocalTime.of(19, 0), "메가박스"),
            ScheduleItem(id(), "브런치 약속", today.plusDays(1), LocalTime.of(9, 0), "브런치카페")
        )
        expenses += listOf(
            ExpenseItem(id(), 12_000, ExpenseCategory.FOOD, 1, "점심 식사", today),
            ExpenseItem(id(), 5_800, ExpenseCategory.CAFE, 2, "카페 라떼", today.minusDays(1)),
            ExpenseItem(id(), 42_000, ExpenseCategory.CLOTHES, 3, "옷 구매", today.minusDays(2))
        )
        incomes += listOf(
            IncomeItem(id(), 200_000, "프로젝트 작업비", today),
            IncomeItem(id(), 150_000, "블로그 광고 수익", today.minusDays(5)),
            IncomeItem(id(), 120_000, "중고 판매", today.minusDays(10))
        )
        store.saveTasks(tasks)
        store.saveSchedules(schedules)
        store.saveExpenses(expenses)
        store.saveIncomes(incomes)
        store.saveCards(cards)
    }
}
