package com.example.mylifetracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TrackerStore(application)
    private val authStore = AuthSessionStore(application)
    private val cloud = FirebaseRestClient(authStore)
    private val cloudMutex = Mutex()

    val tasks = mutableStateListOf<TaskItem>()
    val schedules = mutableStateListOf<ScheduleItem>()
    val expenses = mutableStateListOf<ExpenseItem>()
    val incomes = mutableStateListOf<IncomeItem>()
    val cards = mutableStateListOf<CardProfile>()

    var incomeGoal by mutableIntStateOf(store.loadIncomeGoal())
        private set

    var currentUserEmail by mutableStateOf(authStore.load()?.email)
        private set
    var cloudMessage by mutableStateOf<String?>(null)
        private set
    var isSyncing by mutableStateOf(false)
        private set

    val cloudConfigured: Boolean get() = FirebaseConfig.isConfigured
    val isLoggedIn: Boolean get() = currentUserEmail != null

    private var nextId: Long = System.currentTimeMillis()

    init {
        tasks.addAll(store.loadTasks())
        schedules.addAll(store.loadSchedules())
        expenses.addAll(store.loadExpenses())
        incomes.addAll(store.loadIncomes())
        cards.addAll(store.loadCards().ifEmpty { TrackerStore.defaultCards() })

        repairDuplicateIds()
        if (!store.isInitialized()) store.markInitialized()
        refreshIdFloor()
        TrackerWidgetProvider.updateAll(application)

        if (authStore.load() != null && cloudConfigured) {
            viewModelScope.launch { syncExistingSession() }
        }
    }

    private fun refreshIdFloor() {
        val maximum = buildList<Long> {
            addAll(tasks.map { it.id })
            addAll(schedules.map { it.id })
            addAll(expenses.map { it.id })
            addAll(incomes.map { it.id })
        }.maxOrNull() ?: 0L
        nextId = maxOf(nextId, maximum)
    }

    private fun id(): Long {
        nextId = maxOf(nextId + 1L, System.currentTimeMillis())
        return nextId
    }

    /** Repairs IDs created by the old millisecond+random generator. */
    private fun repairDuplicateIds() {
        var changed = false
        var repairId = listOf(
            System.currentTimeMillis(),
            tasks.maxOfOrNull { it.id } ?: 0L,
            schedules.maxOfOrNull { it.id } ?: 0L,
            expenses.maxOfOrNull { it.id } ?: 0L,
            incomes.maxOfOrNull { it.id } ?: 0L
        ).maxOrNull()!! + 1L

        fun nextRepairId(): Long = repairId++

        run {
            val seen = mutableSetOf<Long>()
            for (i in tasks.indices) {
                if (!seen.add(tasks[i].id)) {
                    tasks[i] = tasks[i].copy(id = nextRepairId())
                    changed = true
                }
            }
        }
        run {
            val seen = mutableSetOf<Long>()
            for (i in schedules.indices) {
                if (!seen.add(schedules[i].id)) {
                    schedules[i] = schedules[i].copy(id = nextRepairId())
                    changed = true
                }
            }
        }
        run {
            val seen = mutableSetOf<Long>()
            for (i in expenses.indices) {
                if (!seen.add(expenses[i].id)) {
                    expenses[i] = expenses[i].copy(id = nextRepairId())
                    changed = true
                }
            }
        }
        run {
            val seen = mutableSetOf<Long>()
            for (i in incomes.indices) {
                if (!seen.add(incomes[i].id)) {
                    incomes[i] = incomes[i].copy(id = nextRepairId())
                    changed = true
                }
            }
        }

        if (changed) persistLocal(touch = true, sync = false)
    }

    fun addTask(title: String, category: TaskCategory, dueDate: LocalDate?) {
        if (title.isBlank()) return
        tasks += TaskItem(id(), title.trim(), category, dueDate)
        persistLocal()
    }

    fun toggleTask(taskId: Long) {
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) tasks[index] = tasks[index].copy(completed = !tasks[index].completed)
        persistLocal()
    }

    fun deleteTask(taskId: Long) {
        tasks.removeAll { it.id == taskId }
        persistLocal()
    }

    fun addSchedule(title: String, date: LocalDate, time: LocalTime, place: String) {
        if (title.isBlank()) return
        schedules += ScheduleItem(id(), title.trim(), date, time, place.trim())
        persistLocal()
    }

    fun deleteSchedule(itemId: Long) {
        schedules.removeAll { it.id == itemId }
        persistLocal()
    }

    fun addExpense(amount: Int, category: ExpenseCategory, cardId: Int, memo: String) {
        if (amount <= 0) return
        expenses += ExpenseItem(id(), amount, category, cardId, memo.trim(), LocalDate.now())
        persistLocal()
    }

    fun deleteExpense(itemId: Long) {
        expenses.removeAll { it.id == itemId }
        persistLocal()
    }

    fun addIncome(amount: Int, memo: String) {
        if (amount <= 0) return
        incomes += IncomeItem(id(), amount, memo.trim(), LocalDate.now())
        persistLocal()
    }

    fun deleteIncome(itemId: Long) {
        incomes.removeAll { it.id == itemId }
        persistLocal()
    }

    fun updateIncomeGoal(value: Int) {
        if (value <= 0) return
        incomeGoal = value
        persistLocal()
    }

    fun updateCard(card: CardProfile) {
        val index = cards.indexOfFirst { it.id == card.id }
        if (index >= 0) cards[index] = card
        persistLocal()
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

    private fun persistLocal(touch: Boolean = true, sync: Boolean = true) {
        store.saveTasks(tasks)
        store.saveSchedules(schedules)
        store.saveExpenses(expenses)
        store.saveIncomes(incomes)
        store.saveCards(cards)
        store.saveIncomeGoal(incomeGoal)
        if (touch) store.setLastModified(System.currentTimeMillis())
        TrackerWidgetProvider.updateAll(getApplication())
        if (sync && isLoggedIn && cloudConfigured) pushLatestInBackground()
    }

    private fun applySnapshot(snapshot: TrackerSnapshot) {
        tasks.clear(); tasks.addAll(snapshot.tasks)
        schedules.clear(); schedules.addAll(snapshot.schedules)
        expenses.clear(); expenses.addAll(snapshot.expenses)
        incomes.clear(); incomes.addAll(snapshot.incomes)
        cards.clear(); cards.addAll(snapshot.cards.ifEmpty { TrackerStore.defaultCards() })
        incomeGoal = snapshot.incomeGoal.coerceAtLeast(1)
        store.replaceWith(snapshot)
        repairDuplicateIds()
        refreshIdFloor()
        TrackerWidgetProvider.updateAll(getApplication())
    }

    private fun currentSnapshot(): TrackerSnapshot = TrackerSnapshot(
        tasks = tasks.toList(),
        schedules = schedules.toList(),
        expenses = expenses.toList(),
        incomes = incomes.toList(),
        cards = cards.toList(),
        incomeGoal = incomeGoal,
        updatedAt = store.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis()
    )

    fun signIn(email: String, password: String) {
        if (!cloudConfigured) {
            cloudMessage = "먼저 Firebase 클라우드 설정을 연결해 주세요."
            return
        }
        isSyncing = true
        cloudMessage = null
        viewModelScope.launch {
            val result = cloud.signIn(email, password)
            if (result.isFailure) {
                cloudMessage = result.exceptionOrNull()?.message ?: "로그인에 실패했어요."
                isSyncing = false
                return@launch
            }
            currentUserEmail = result.getOrNull()?.email
            syncAfterExplicitLogin()
        }
    }

    fun signUp(email: String, password: String) {
        if (!cloudConfigured) {
            cloudMessage = "먼저 Firebase 클라우드 설정을 연결해 주세요."
            return
        }
        isSyncing = true
        cloudMessage = null
        viewModelScope.launch {
            val result = cloud.signUp(email, password)
            if (result.isFailure) {
                cloudMessage = result.exceptionOrNull()?.message ?: "계정 생성에 실패했어요."
                isSyncing = false
                return@launch
            }
            currentUserEmail = result.getOrNull()?.email
            cloudMutex.withLock {
                val upload = cloud.uploadSnapshot(currentSnapshot())
                cloudMessage = if (upload.isSuccess) "계정을 만들고 현재 데이터를 클라우드에 저장했어요." else upload.exceptionOrNull()?.message
            }
            isSyncing = false
        }
    }

    /** Explicit login always prefers an existing cloud copy, so reinstall/new-device login restores old data. */
    private suspend fun syncAfterExplicitLogin() {
        cloudMutex.withLock {
            val remoteResult = cloud.downloadSnapshot()
            if (remoteResult.isFailure) {
                cloudMessage = remoteResult.exceptionOrNull()?.message
                isSyncing = false
                return
            }
            val remote = remoteResult.getOrNull()
            if (remote != null) {
                applySnapshot(remote)
                cloudMessage = "클라우드에 저장된 데이터를 불러왔어요."
            } else {
                val snapshot = currentSnapshot().copy(updatedAt = System.currentTimeMillis())
                store.setLastModified(snapshot.updatedAt)
                val upload = cloud.uploadSnapshot(snapshot)
                cloudMessage = if (upload.isSuccess) "현재 데이터를 클라우드에 처음 저장했어요." else upload.exceptionOrNull()?.message
            }
        }
        isSyncing = false
    }

    private suspend fun syncExistingSession() {
        isSyncing = true
        cloudMutex.withLock {
            val remoteResult = cloud.downloadSnapshot()
            if (remoteResult.isFailure) {
                cloudMessage = remoteResult.exceptionOrNull()?.message
                isSyncing = false
                return
            }
            val remote = remoteResult.getOrNull()
            val local = currentSnapshot()
            when {
                remote == null -> cloud.uploadSnapshot(local)
                remote.updatedAt > local.updatedAt -> applySnapshot(remote)
                local.updatedAt > remote.updatedAt -> cloud.uploadSnapshot(local)
            }
            currentUserEmail = authStore.load()?.email
            cloudMessage = "클라우드 동기화 완료"
        }
        isSyncing = false
    }

    fun syncNow() {
        if (!isLoggedIn || !cloudConfigured) return
        isSyncing = true
        cloudMessage = null
        viewModelScope.launch { syncExistingSession() }
    }

    private fun pushLatestInBackground() {
        viewModelScope.launch {
            cloudMutex.withLock {
                val snapshot = currentSnapshot()
                val result = cloud.uploadSnapshot(snapshot)
                cloudMessage = if (result.isSuccess) "저장됨" else result.exceptionOrNull()?.message
            }
        }
    }

    fun logout() {
        authStore.clear()
        currentUserEmail = null
        cloudMessage = "로그아웃했어요. 기기 안의 데이터는 그대로 유지돼요."
    }

    fun clearCloudMessage() {
        cloudMessage = null
    }
}
