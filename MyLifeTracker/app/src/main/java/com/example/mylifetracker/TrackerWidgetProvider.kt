package com.example.mylifetracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

class TrackerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TrackerWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val store = TrackerStore(context)
            val today = LocalDate.now()
            val tasks = store.loadTasks().count { !it.completed }
            val schedules = store.loadSchedules().count { it.date == today }
            val expense = store.loadExpenses()
                .filter { it.date.year == today.year && it.date.month == today.month }
                .sumOf { it.amount }
            val income = store.loadIncomes()
                .filter { it.date.year == today.year && it.date.month == today.month }
                .sumOf { it.amount }
            val goal = store.loadIncomeGoal().coerceAtLeast(1)
            val progress = ((income.toDouble() / goal) * 100.0).roundToInt().coerceIn(0, 100)

            val views = RemoteViews(context.packageName, R.layout.tracker_widget).apply {
                setTextViewText(R.id.widgetTasks, "할 일 ${tasks}개")
                setTextViewText(R.id.widgetSchedule, "오늘 일정 ${schedules}개")
                setTextViewText(R.id.widgetExpense, "소비 ${money(expense)}")
                setTextViewText(R.id.widgetIncome, "수입 ${money(income)}")
                setTextViewText(R.id.widgetIncomePercent, "수입 목표 $progress%")
                setProgressBar(R.id.widgetIncomeProgress, 100, progress, false)

                val intent = Intent(context, MainActivity::class.java)
                val pending = PendingIntent.getActivity(
                    context,
                    100,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widgetRoot, pending)
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun money(value: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"
    }
}
