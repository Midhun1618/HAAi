package com.voxcom.haai

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

data class PillRow(
    val title: String,
    val subtitle: String,
    val status: DoseState? = null,
    val dose: ScheduledDose? = null,
    val medicine: Medicine? = null,
    val showActions: Boolean = false,
    val showDelete: Boolean = false
)

class PillRowAdapter(
    private val onTaken: (ScheduledDose) -> Unit,
    private val onSkip: (ScheduledDose) -> Unit,
    private val onOpen: (Medicine) -> Unit,
    private val onDelete: (Medicine) -> Unit
) : RecyclerView.Adapter<PillRowAdapter.VH>() {

    private var rows: List<PillRow> = emptyList()

    fun submit(list: List<PillRow>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.rowTitle)
        val subtitle: TextView = v.findViewById(R.id.rowSubtitle)
        val status: TextView = v.findViewById(R.id.rowStatus)
        val delete: ImageView = v.findViewById(R.id.rowDelete)
        val actionRow: View = v.findViewById(R.id.rowActions)
        val btnTaken: MaterialButton = v.findViewById(R.id.rowBtnTaken)
        val btnSkip: MaterialButton = v.findViewById(R.id.rowBtnSkip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_pill_row, parent, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        val ctx = h.itemView.context
        h.title.text = r.title
        h.subtitle.text = r.subtitle

        val s = r.status
        if (s != null) {
            val (label, fg, bg) = when (s) {
                DoseState.TAKEN -> Triple(R.string.pill_status_taken, R.color.haai_success, R.color.haai_success_soft)
                DoseState.SKIPPED -> Triple(R.string.pill_status_skipped, R.color.haai_text_secondary, R.color.haai_stroke)
                DoseState.DUE -> Triple(R.string.pill_status_due, R.color.haai_warning, R.color.haai_warning_soft)
                DoseState.MISSED -> Triple(R.string.pill_status_missed, R.color.haai_emergency, R.color.haai_emergency_soft)
                DoseState.UPCOMING -> Triple(R.string.pill_status_upcoming, R.color.primary_blue_dark, R.color.haai_accent_soft)
            }
            h.status.visibility = View.VISIBLE
            h.status.setText(label)
            h.status.setTextColor(ContextCompat.getColor(ctx, fg))
            h.status.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
        } else {
            h.status.visibility = View.GONE
        }

        h.delete.visibility = if (r.showDelete) View.VISIBLE else View.GONE
        h.delete.setOnClickListener { r.medicine?.let(onDelete) }

        h.actionRow.visibility = if (r.showActions) View.VISIBLE else View.GONE
        h.btnTaken.setOnClickListener { r.dose?.let(onTaken) }
        h.btnSkip.setOnClickListener { r.dose?.let(onSkip) }

        if (r.medicine != null) {
            h.itemView.setOnClickListener { onOpen(r.medicine) }
        } else {
            h.itemView.setOnClickListener(null)
            h.itemView.isClickable = false
        }
    }
}
