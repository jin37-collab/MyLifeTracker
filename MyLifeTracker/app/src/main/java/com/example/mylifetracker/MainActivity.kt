package com.example.mylifetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Blue = Color(0xFF4D8FE8)
private val Green = Color(0xFF52B58B)
private val Red = Color(0xFFE95E67)
private val Purple = Color(0xFF8E73DE)
private val Ink = Color(0xFF101426)
private val Muted = Color(0xFF747B8F)
private val AppBg = Color(0xFFFAFAFC)
private val CardBg = Color.White
private val Border = Color(0xFFE9EBF1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Blue,
                    secondary = Green,
                    background = AppBg,
                    surface = CardBg,
                    onSurface = Ink
                )
            ) {
                TrackerApp()
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Default.Home),
    TASKS("할 일", Icons.Default.CheckCircle),
    SCHEDULE("일정", Icons.Default.CalendarMonth),
    EXPENSE("소비", Icons.Default.CreditCard),
    INCOME("수입", Icons.Default.BarChart)
}

@Composable
private fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    Scaffold(
        containerColor = AppBg,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 3.dp) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = when (item) {
                                AppTab.INCOME -> Green
                                else -> Blue
                            },
                            selectedTextColor = when (item) {
                                AppTab.INCOME -> Green
                                else -> Blue
                            },
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                AppTab.HOME -> HomeScreen(vm) { tab = it }
                AppTab.TASKS -> TasksScreen(vm)
                AppTab.SCHEDULE -> ScheduleScreen(vm)
                AppTab.EXPENSE -> ExpenseScreen(vm)
                AppTab.INCOME -> IncomeScreen(vm)
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(vm: TrackerViewModel, onTab: (AppTab) -> Unit) {
    val today = LocalDate.now()
    val pending = vm.tasks.count { !it.completed }
    val todaySchedules = vm.schedules.count { it.date == today }
    val monthExpense = vm.monthExpense(today)
    val monthIncome = vm.monthIncome(today)
    val urgentTasks = vm.tasks.filter { !it.completed && it.dueDate != null }
        .sortedBy { it.dueDate }.take(3)
    val schedules = vm.schedules.filter { it.date == today }.sortedBy { it.time }.take(4)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("홈") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("이번 달 요약")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("할 일", "${pending}개", Icons.Default.CheckCircle, Blue, Modifier.weight(1f)) { onTab(AppTab.TASKS) }
                    SummaryTile("오늘 일정", "${todaySchedules}개", Icons.Default.CalendarMonth, Blue, Modifier.weight(1f)) { onTab(AppTab.SCHEDULE) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("이번 달 소비", money(monthExpense), Icons.Default.CreditCard, Red, Modifier.weight(1f)) { onTab(AppTab.EXPENSE) }
                    SummaryTile("이번 달 수입", money(monthIncome), Icons.Default.BarChart, Green, Modifier.weight(1f)) { onTab(AppTab.INCOME) }
                }
            }
        }
        item {
            HomeCard("마감 임박 할 일", onMore = { onTab(AppTab.TASKS) }) {
                if (urgentTasks.isEmpty()) EmptyLine("마감일이 있는 할 일이 없어요")
                urgentTasks.forEach { task ->
                    CompactTaskRow(task, onToggle = { vm.toggleTask(task.id) })
                }
            }
        }
        item {
            HomeCard("오늘 일정", onMore = { onTab(AppTab.SCHEDULE) }) {
                if (schedules.isEmpty()) EmptyLine("오늘 등록된 일정이 없어요")
                schedules.forEach { item -> CompactScheduleRow(item) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardProgressMini(vm, Modifier.weight(1.55f)) { onTab(AppTab.EXPENSE) }
                IncomeProgressMini(vm, Modifier.weight(1f)) { onTab(AppTab.INCOME) }
            }
        }
    }
}

@Composable
private fun SummaryTile(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = color.copy(alpha = 0.07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
        }
    }
}

@Composable
private fun HomeCard(title: String, onMore: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(title)
                if (onMore != null) Text("더보기 ›", color = Muted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onMore))
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun CompactTaskRow(task: TaskItem, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.RadioButtonUnchecked, null, tint = Blue)
        }
        Text(task.title, modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        task.dueDate?.let { DueBadge(it) }
    }
}

@Composable
private fun CompactScheduleRow(item: ScheduleItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(item.time.format(DateTimeFormatter.ofPattern("HH:mm")), color = Muted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
        Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(Blue.copy(alpha = .55f)))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (item.place.isNotBlank()) Text(item.place, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CardProgressMini(vm: TrackerViewModel, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(14.dp)) {
            SectionLabel("카드 실적")
            Spacer(Modifier.height(8.dp))
            vm.cards.forEach { card ->
                val spent = vm.cardSpent(card.id)
                val progress = if (card.monthlyGoal == 0) 0f else (spent.toFloat() / card.monthlyGoal).coerceIn(0f, 1f)
                Text(card.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (card.id == 2) Purple else if (card.id == 3) Green else Blue,
                    trackColor = Border
                )
                Text("${(progress * 100).roundToInt()}% 달성 · ${100 - (progress * 100).roundToInt()}% 남음", color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun IncomeProgressMini(vm: TrackerViewModel, modifier: Modifier, onClick: () -> Unit) {
    val progress = if (vm.incomeGoal <= 0) 0f else (vm.monthIncome().toFloat() / vm.incomeGoal).coerceIn(0f, 1f)
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel("수입 목표")
            Spacer(Modifier.height(9.dp))
            ProgressRing(progress, 95.dp, Green) {
                Text("${(progress * 100).roundToInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Text("${money(vm.monthIncome())} / ${money(vm.incomeGoal)}", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun TasksScreen(vm: TrackerViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<TaskCategory?>(null) }
    val active = vm.tasks.filter { !it.completed && (filter == null || it.category == filter) }
    val due = active.filter { it.dueDate != null }.sortedWith(compareBy<TaskItem> { it.dueDate }.thenBy { it.title })
    val noDue = active.filter { it.dueDate == null }.sortedBy { it.title }
    val completed = vm.tasks.filter { it.completed && (filter == null || it.category == filter) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("할 일 관리하기") {
            FilledIconButton(onClick = { showAdd = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SmallFilterChip("전체", filter == null) { filter = null }
            TaskCategory.entries.forEach { c -> SmallFilterChip(c.label, filter == c) { filter = c } }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (due.isNotEmpty()) {
                item { GroupHeader("마감 일정 ${due.size}", "기한이 임박한 할 일이 먼저 보여요") }
                items(due, key = { it.id }) { task -> TaskRow(task, vm::toggleTask, vm::deleteTask) }
            }
            item { GroupHeader("일반 할 일 ${noDue.size}", "마감일이 없는 할 일이에요") }
            if (noDue.isEmpty()) item { EmptyCard("마감일 없는 할 일이 없어요") }
            items(noDue, key = { it.id }) { task -> TaskRow(task, vm::toggleTask, vm::deleteTask) }
            if (completed.isNotEmpty()) {
                item { GroupHeader("완료 ${completed.size}", "완료한 할 일이에요") }
                items(completed, key = { it.id }) { task -> TaskRow(task, vm::toggleTask, vm::deleteTask) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (showAdd) AddTaskDialog(onDismiss = { showAdd = false }) { title, category, date ->
        vm.addTask(title, category, date)
        showAdd = false
    }
}

@Composable
private fun SmallFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Blue,
            selectedLabelColor = Color.White,
            containerColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Border, selectedBorderColor = Blue)
    )
}

@Composable
private fun GroupHeader(title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Blue.copy(alpha = .06f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventAvailable, null, tint = Blue)
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem, onToggle: (Long) -> Unit, onDelete: (Long) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggle(task.id) }) {
                Icon(if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (task.completed) Blue else Muted)
            }
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, color = if (task.completed) Muted else Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.category.label, color = categoryColor(task.category), fontSize = 11.sp)
                    task.dueDate?.let {
                        Text("  ·  ", color = Muted)
                        Text(dateShort(it), color = Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            IconButton(onClick = { onDelete(task.id) }) { Icon(Icons.Default.DeleteOutline, null, tint = Muted) }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, TaskCategory, LocalDate?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.PERSONAL) }
    var dueText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("할 일 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("할 일") }, singleLine = true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskCategory.entries.forEach { c -> SmallFilterChip(c.label, category == c) { category = c } }
                }
                OutlinedTextField(
                    dueText,
                    { dueText = it },
                    label = { Text("마감일 (선택)") },
                    placeholder = { Text("2026-09-30") },
                    singleLine = true,
                    supportingText = { Text("비워두면 마감일 없는 할 일로 저장돼요") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val date = dueText.trim().takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                onSave(title, category, date)
            }, enabled = title.isNotBlank()) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun ScheduleScreen(vm: TrackerViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val dates = remember { (0L..180L).map { today.plusDays(it) } }
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("일정") {
            FilledIconButton(onClick = { showAdd = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Blue)) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dates.forEach { date ->
                val dayItems = vm.schedules.filter { it.date == date }.sortedBy { it.time }
                item(key = "head-$date") {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(date.format(formatter), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        if (date == today) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = Blue) { Text("오늘", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
                        }
                    }
                }
                if (dayItems.isEmpty()) {
                    item(key = "empty-$date") { EmptyScheduleDay() }
                } else {
                    items(dayItems, key = { it.id }) { item -> ScheduleRow(item, vm::deleteSchedule) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (showAdd) AddScheduleDialog(onDismiss = { showAdd = false }) { title, date, time, place ->
        vm.addSchedule(title, date, time, place)
        showAdd = false
    }
}

@Composable
private fun ScheduleRow(item: ScheduleItem, onDelete: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 66.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(item.time.format(DateTimeFormatter.ofPattern("HH:mm")), color = Muted, fontSize = 12.sp, modifier = Modifier.width(52.dp))
        Box(Modifier.width(3.dp).fillMaxHeight().height(58.dp).clip(CircleShape).background(Blue.copy(alpha = .45f)))
        Spacer(Modifier.width(10.dp))
        Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Blue.copy(alpha = .08f)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (item.place.isNotBlank()) Text(item.place, color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(17.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyScheduleDay() {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(62.dp))
        Text("등록된 일정 없음", color = Muted.copy(alpha = .65f), fontSize = 12.sp)
    }
}

@Composable
private fun AddScheduleDialog(onDismiss: () -> Unit, onSave: (String, LocalDate, LocalTime, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf("09:00") }
    var place by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("일정") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("날짜") }, placeholder = { Text("2026-09-21") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("시간") }, placeholder = { Text("14:00") }, singleLine = true)
                OutlinedTextField(place, { place = it }, label = { Text("장소 (선택)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@Button
                val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return@Button
                onSave(title, parsedDate, parsedTime, place)
            }, enabled = title.isNotBlank()) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun ExpenseScreen(vm: TrackerViewModel) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var cardId by remember { mutableIntStateOf(vm.cards.firstOrNull()?.id ?: 1) }
    var memo by remember { mutableStateOf("") }
    var showCardSettings by remember { mutableStateOf(false) }
    val recent = vm.expenses.sortedByDescending { it.id }.take(5)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenTitle("소비 기록하기") }
        item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("소비 금액", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it.filter(Char::isDigit).take(9) },
                                placeholder = { Text("0원") },
                                suffix = { Text("원") },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        VerticalDivider(Modifier.height(208.dp), color = Border)
                        Column(Modifier.weight(1f)) {
                            Text("카테고리", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            ExpenseCategory.entries.forEach { c ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { category = c }.padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = category == c, onClick = { category = c })
                                    Text(c.label, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Border)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("사용한 카드", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("카드 설정", color = Blue, fontSize = 12.sp, modifier = Modifier.clickable { showCardSettings = true })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.cards.forEach { card ->
                            FilterChip(
                                selected = card.id == cardId,
                                onClick = { cardId = card.id },
                                label = { Text(card.name, maxLines = 1, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White)
                            )
                        }
                    }
                    OutlinedTextField(memo, { memo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("메모 (선택)") }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toIntOrNull() ?: 0
                            vm.addExpense(amount, category, cardId, memo)
                            amountText = ""
                            memo = ""
                        },
                        enabled = (amountText.toIntOrNull() ?: 0) > 0,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("기록하기") }
                }
            }
        }
        item {
            HomeCard("최근 소비 내역") {
                if (recent.isEmpty()) EmptyLine("아직 소비 내역이 없어요")
                recent.forEach { expense -> ExpenseHistoryRow(expense, vm.cards.firstOrNull { it.id == expense.cardId }?.name ?: "카드", vm::deleteExpense) }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionLabel("카드 실적 현황")
                        Text("이번 달", color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    vm.cards.forEach { card -> CardProgressRow(card, vm.cardSpent(card.id)) }
                }
            }
        }
    }
    if (showCardSettings) CardSettingsDialog(vm.cards.toList(), onDismiss = { showCardSettings = false }) { changed ->
        changed.forEach(vm::updateCard)
        showCardSettings = false
    }
}

@Composable
private fun ExpenseHistoryRow(item: ExpenseItem, cardName: String, onDelete: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dateTiny(item.date), color = Muted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(item.memo.ifBlank { item.category.label }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.category.label} · $cardName", color = Muted, fontSize = 10.sp)
        }
        Text("-${money(item.amount)}", color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(15.dp)) }
    }
}

@Composable
private fun CardProgressRow(card: CardProfile, spent: Int) {
    val raw = if (card.monthlyGoal <= 0) 0f else spent.toFloat() / card.monthlyGoal
    val progress = raw.coerceIn(0f, 1f)
    val percent = (raw * 100).roundToInt().coerceAtLeast(0)
    val remaining = (100 - percent).coerceAtLeast(0)
    val color = when (card.id) { 2 -> Purple; 3 -> Green; else -> Blue }
    Column(Modifier.padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${money(spent)} / ${money(card.monthlyGoal)}", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = color, trackColor = Border)
        Spacer(Modifier.height(4.dp))
        Text(if (percent >= 100) "목표 달성" else "$percent% 달성 · $remaining% 남음", color = if (percent >= 100) Green else Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CardSettingsDialog(cards: List<CardProfile>, onDismiss: () -> Unit, onSave: (List<CardProfile>) -> Unit) {
    val names = remember(cards) { cards.map { mutableStateOf(it.name) } }
    val goals = remember(cards) { cards.map { mutableStateOf(it.monthlyGoal.toString()) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카드 설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards.forEachIndexed { index, _ ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(names[index].value, { names[index].value = it }, label = { Text("카드 ${index + 1}") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(goals[index].value, { goals[index].value = it.filter(Char::isDigit) }, label = { Text("월 실적") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(cards.mapIndexed { i, c -> c.copy(name = names[i].value.ifBlank { c.name }, monthlyGoal = goals[i].value.toIntOrNull()?.coerceAtLeast(1) ?: c.monthlyGoal) })
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun IncomeScreen(vm: TrackerViewModel) {
    var showGoal by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val monthIncome = vm.monthIncome()
    val progress = if (vm.incomeGoal <= 0) 0f else (monthIncome.toFloat() / vm.incomeGoal).coerceIn(0f, 1f)
    val recent = vm.incomes.sortedByDescending { it.id }.take(7)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenTitle("수입 관리하기") {
                FilledIconButton(onClick = { showAdd = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Green)) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("목표 수입 달성률", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    ProgressRing(progress, 190.dp, Green) {
                        Text("${(progress * 100).roundToInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
                        Text("${money(monthIncome)} /", color = Muted, fontSize = 12.sp)
                        Text(money(vm.incomeGoal), color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Green.copy(alpha = .09f)) {
                        Text(if (progress >= 1f) "이번 달 목표를 달성했어요" else "목표까지 ${money((vm.incomeGoal - monthIncome).coerceAtLeast(0))} 남았어요", color = Green, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("수입 목표", color = Muted, fontSize = 11.sp)
                        Text(money(vm.incomeGoal), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(onClick = { showGoal = true }) { Text("목표 수정") }
                }
            }
        }
        item {
            HomeCard("최근 수입 내역") {
                if (recent.isEmpty()) EmptyLine("아직 수입 내역이 없어요")
                recent.forEach { item -> IncomeHistoryRow(item, vm::deleteIncome) }
            }
        }
    }
    if (showGoal) GoalDialog(vm.incomeGoal, { showGoal = false }) { vm.updateIncomeGoal(it); showGoal = false }
    if (showAdd) AddIncomeDialog({ showAdd = false }) { amount, memo -> vm.addIncome(amount, memo); showAdd = false }
}

@Composable
private fun IncomeHistoryRow(item: IncomeItem, onDelete: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dateTiny(item.date), color = Muted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Text(item.memo.ifBlank { "수입" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("+${money(item.amount)}", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(15.dp)) }
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("월 수입 목표") },
        text = { OutlinedTextField(text, { text = it.filter(Char::isDigit) }, label = { Text("목표 금액") }, suffix = { Text("원") }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(text.toIntOrNull() ?: current) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun AddIncomeDialog(onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("수입 기록") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("수입 금액") }, suffix = { Text("원") }, singleLine = true)
                OutlinedTextField(memo, { memo = it }, label = { Text("내용") }, placeholder = { Text("예: 과외비") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(amount.toIntOrNull() ?: 0, memo) }, enabled = (amount.toIntOrNull() ?: 0) > 0) { Text("기록") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun ProgressRing(progress: Float, size: androidx.compose.ui.unit.Dp, color: Color, center: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.toPx() * .075f
            drawArc(color = Border, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color = color, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = center)
    }
}

@Composable private fun SectionLabel(text: String) = Text(text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
@Composable private fun EmptyLine(text: String) = Text(text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
@Composable private fun EmptyCard(text: String) = Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Text(text, color = Muted, modifier = Modifier.fillMaxWidth().padding(16.dp), fontSize = 12.sp) }

@Composable
private fun DueBadge(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "오늘"
        today.plusDays(1) -> "내일"
        else -> dateShort(date)
    }
    Surface(shape = CircleShape, color = Red.copy(alpha = .10f)) { Text(label, color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
}

private fun categoryColor(category: TaskCategory) = when (category) {
    TaskCategory.PERSONAL -> Green
    TaskCategory.STUDY -> Blue
    TaskCategory.WORK -> Purple
    TaskCategory.HEALTH -> Red
}

private fun money(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"
private fun dateShort(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN))
private fun dateTiny(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("M/d"))
