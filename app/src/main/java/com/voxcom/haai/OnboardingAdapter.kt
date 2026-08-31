package com.voxcom.haai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(
    private val items: List<OnboardingData>,
    private val onDisclaimerChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    var isDisclaimerAccepted = false

    inner class OnboardingViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val banner: ImageView =
            itemView.findViewById(R.id.banner)

        private val title: TextView =
            itemView.findViewById(R.id.title)

        private val description: TextView =
            itemView.findViewById(R.id.desc)

        private val checkBox: CheckBox =
            itemView.findViewById(R.id.checkBox)


        fun bind(item: OnboardingData) {

            banner.setImageResource(item.banner)

            title.text = item.title

            description.text = item.description


            if (item.isDisclaimer) {

                checkBox.visibility = View.VISIBLE

                checkBox.setOnCheckedChangeListener(null)

                checkBox.isChecked = isDisclaimerAccepted

                checkBox.setOnCheckedChangeListener { _, isChecked ->

                    isDisclaimerAccepted = isChecked

                    onDisclaimerChanged(isChecked)
                }

            } else {

                checkBox.visibility = View.GONE
            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OnboardingViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_onboarding,
                parent,
                false
            )

        return OnboardingViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: OnboardingViewHolder,
        position: Int
    ) {

        holder.bind(items[position])
    }


    override fun getItemCount(): Int {
        return items.size
    }
}