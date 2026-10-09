package com.voxcom.haai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale
import java.util.UUID

class AddMedicineActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MED_ID = "med_id"
    }

    private lateinit var tilName: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etDose: TextInputEditText
    private lateinit var chipTimes: ChipGroup
    private lateinit var chipDays: ChipGroup
    private lateinit var errorTv: TextView

    private val times = sortedSetOf<String>()
    private var existing: Medicine? = null

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, getString(R.string.pill_notif_denied), Toast.LENGTH_LONG).show()
            }
            finish()
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_medicine)

        val root = findViewById<View>(R.id.addRoot)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            header.setPadding(header.paddingLeft, bars.top + dp(16), header.paddingRight, header.paddingBottom)
            v.setPadding(0, 0, 0, maxOf(bars.bottom, ime.bottom))
            insets
        }

        tilName = findViewById(R.id.tilName)
        etName = findViewById(R.id.etName)
        etDose = findViewById(R.id.etDose)
        chipTimes = findViewById(R.id.chipTimes)
        chipDays = findViewById(R.id.chipDays)
        errorTv = findViewById(R.id.errorTv)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddTime).setOnClickListener { pickTime() }
        findViewById<View>(R.id.btnSave).setOnClickListener { save() }

        val id = intent.getStringExtra(EXTRA_MED_ID)
        if (id != null) {
            existing = PillStore.getMedicine(this, id)
        }
        existing?.let { m ->
            findViewById<TextView>(R.id.titleTv).setText(R.string.pill_edit_medicine)
            etName.setText(m.name)
            etDose.setText(m.dose)
            times.addAll(m.times)
            for (i in 0 until chipDays.childCount) {
                val chip = chipDays.getChildAt(i) as Chip
                chip.isChecked = m.days.isEmpty() || (chip.tag.toString().toInt() in m.days)
            }
        }
        renderTimes()
    }

    private fun renderTimes() {
        chipTimes.removeAllViews()
        for (t in times) {
            val chip = layoutInflater.inflate(R.layout.item_time_chip, chipTimes, false) as Chip
            chip.text = DoseSchedule.display(this, t)
            chip.setOnCloseIconClickListener {
                times.remove(t)
                renderTimes()
            }
            chipTimes.addView(chip)
        }
    }

    private fun pickTime() {
        val fmt = if (android.text.format.DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(fmt)
            .setHour(8)
            .setMinute(0)
            .setTitleText(getString(R.string.pill_pick_time))
            .build()
        picker.addOnPositiveButtonClickListener {
            times.add(String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute))
            errorTv.visibility = View.GONE
            renderTimes()
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun showError(res: Int) {
        errorTv.setText(res)
        errorTv.visibility = View.VISIBLE
    }

    private fun save() {
        tilName.error = null
        errorTv.visibility = View.GONE

        val name = etName.text?.toString()?.trim().orEmpty()
        val dose = etDose.text?.toString()?.trim().orEmpty()
        val days = mutableSetOf<Int>()
        for (i in 0 until chipDays.childCount) {
            val chip = chipDays.getChildAt(i) as Chip
            if (chip.isChecked) days.add(chip.tag.toString().toInt())
        }

        if (name.isEmpty()) {
            tilName.error = getString(R.string.pill_err_name)
            return
        }
        if (times.isEmpty()) {
            showError(R.string.pill_err_times)
            return
        }
        if (days.isEmpty()) {
            showError(R.string.pill_err_days)
            return
        }

        val old = existing
        val now = System.currentTimeMillis()
        val timeList = times.toList()
        // Keep history when only the name/dose changed; restart tracking if the schedule changed.
        val createdAt = if (old != null && old.times.toSet() == times.toSet() && old.days == days) old.createdAt else now

        val med = Medicine(
            id = old?.id ?: UUID.randomUUID().toString(),
            name = name,
            dose = dose,
            times = timeList,
            days = days,
            createdAt = createdAt
        )

        if (old != null) ReminderScheduler.cancel(this, old)
        PillStore.upsert(this, med)
        ReminderScheduler.schedule(this, med)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish()
        }
    }
}
