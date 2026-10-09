package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Calendar

class PillTrackerActivity : AppCompatActivity() {

    private enum class Tab { TODAY, MEDICINES, HISTORY }

    private var tab = Tab.TODAY
    private lateinit var adapter: PillRowAdapter
    private lateinit var rowsView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyBody: TextView
    private lateinit var summaryTv: TextView
    private lateinit var notifBanner: View

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pill_tracker)

        val root = findViewById<View>(R.id.pillTrackerRoot)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.setPadding(header.paddingLeft, bars.top + dp(16), header.paddingRight, header.paddingBottom)
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        rowsView = findViewById(R.id.rows)
        emptyState = findViewById(R.id.emptyState)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptyBody = findViewById(R.id.emptyBody)
        summaryTv = findViewById(R.id.summaryTv)
        notifBanner = findViewById(R.id.notifBanner)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }
        notifBanner.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }

        adapter = PillRowAdapter(
            onTaken = { logDose(it, DoseStatus.TAKEN) },
            onSkip = { logDose(it, DoseStatus.SKIPPED) },
            onOpen = {
                startActivity(
                    Intent(this, AddMedicineActivity::class.java)
                        .putExtra(AddMedicineActivity.EXTRA_MED_ID, it.id)
                )
            },
            onDelete = { confirmDelete(it) }
        )
        rowsView.layoutManager = LinearLayoutManager(this)
        rowsView.adapter = adapter

        val tabs = findViewById<MaterialButtonToggleGroup>(R.id.tabGroup)
        tabs.check(R.id.tabToday)
        tabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tab = when (checkedId) {
                R.id.tabMedicines -> Tab.MEDICINES
                R.id.tabHistory -> Tab.HISTORY
                else -> Tab.TODAY
            }
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        val hasMeds = PillStore.getMedicines(this).isNotEmpty()
        notifBanner.visibility =
            if (hasMeds && !NotificationManagerCompat.from(this).areNotificationsEnabled()) View.VISIBLE else View.GONE
        render()
    }

    private fun logDose(d: ScheduledDose, status: DoseStatus) {
        PillStore.logDose(this, d.medicine.id, d.scheduledAt, status)
        NotificationHelper.cancel(this, d.medicine.id, d.scheduledAt)
        render()
    }

    private fun confirmDelete(m: Medicine) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pill_delete_title))
            .setMessage(getString(R.string.pill_delete_message, m.name))
            .setPositiveButton(getString(R.string.pill_delete_yes)) { _, _ ->
                ReminderScheduler.cancel(this, m)
                PillStore.delete(this, m.id)
                render()
            }
            .setNegativeButton(getString(R.string.pill_cancel), null)
            .show()
    }

    private fun render() {
        val now = System.currentTimeMillis()
        val meds = PillStore.getMedicines(this)
        val rows: List<PillRow>

        when (tab) {
            Tab.TODAY -> {
                val doses = DoseSchedule.dosesForDay(this, now, now)
                rows = doses.map { d ->
                    PillRow(
                        title = d.medicine.name,
                        subtitle = listOf(d.medicine.dose, DoseSchedule.display(this, d.time))
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        status = d.state,
                        dose = d,
                        showActions = DoseSchedule.canLog(d, now)
                    )
                }
                summaryTv.text = when {
                    meds.isEmpty() -> getString(R.string.pill_summary_none)
                    doses.isEmpty() -> getString(R.string.pill_summary_nothing_today)
                    else -> getString(
                        R.string.pill_summary_today,
                        doses.count { it.state == DoseState.TAKEN }, doses.size
                    )
                }
                if (meds.isEmpty()) setEmpty(R.string.pill_empty_title, R.string.pill_empty_body)
                else setEmpty(R.string.pill_empty_today_title, R.string.pill_empty_today_body)
            }

            Tab.MEDICINES -> {
                rows = meds.map { m ->
                    val times = m.times.sorted().joinToString(", ") { DoseSchedule.display(this, it) }
                    PillRow(
                        title = m.name,
                        subtitle = listOf(m.dose, times, DoseSchedule.daysSummary(m.days))
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        medicine = m,
                        showDelete = true
                    )
                }
                summaryTv.text = resources.getQuantityString(R.plurals.pill_summary_meds, meds.size, meds.size)
                setEmpty(R.string.pill_empty_title, R.string.pill_empty_body)
            }

            Tab.HISTORY -> {
                val all = mutableListOf<ScheduledDose>()
                for (back in 0..6) {
                    val dayMillis = Calendar.getInstance().apply {
                        timeInMillis = now
                        add(Calendar.DAY_OF_YEAR, -back)
                    }.timeInMillis
                    all += DoseSchedule.dosesForDay(this, dayMillis, now)
                        .filter { it.state == DoseState.TAKEN || it.state == DoseState.SKIPPED || it.state == DoseState.MISSED }
                }
                val sorted = all.sortedByDescending { it.scheduledAt }
                rows = sorted.map { d ->
                    PillRow(
                        title = d.medicine.name,
                        subtitle = DateUtils.formatDateTime(
                            this, d.scheduledAt,
                            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or
                                    DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
                        ),
                        status = d.state
                    )
                }
                val taken = sorted.count { it.state == DoseState.TAKEN }
                summaryTv.text = if (sorted.isEmpty()) getString(R.string.pill_summary_history_none)
                else getString(R.string.pill_summary_history, taken, sorted.size, taken * 100 / sorted.size)
                setEmpty(R.string.pill_empty_history_title, R.string.pill_empty_history_body)
            }
        }

        adapter.submit(rows)
        emptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        rowsView.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setEmpty(title: Int, body: Int) {
        emptyTitle.setText(title)
        emptyBody.setText(body)
    }
}
