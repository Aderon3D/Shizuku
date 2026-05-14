package moe.shizuku.manager.start.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.databinding.ItemStartStepBinding
import moe.shizuku.manager.start.models.StartStepItem
import moe.shizuku.manager.start.models.StartStepUiStatus

class StartStepAdapter :
    androidx.recyclerview.widget.ListAdapter<StartStepItem, StartStepAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemStartStepBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStartStepBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StartStepItem) {
            binding.label.setText(item.label)
            binding.icon.setImageResource(item.icon)

            val status = item.status
            binding.statusPending.isVisible = status is StartStepUiStatus.Pending
            binding.statusRunning.isVisible = status is StartStepUiStatus.Running
            binding.statusCompleted.isVisible = status is StartStepUiStatus.Completed
            binding.statusFailed.isVisible = status is StartStepUiStatus.Failed

            when (status) {
                is StartStepUiStatus.Failed -> {
                    binding.userErrorMessage.isVisible = true
                    binding.userErrorMessage.text = status.message.asString(binding.root.context)
                    binding.actionButton.isVisible = status.actionText != null
                    binding.actionButton.text = status.actionText
                    binding.actionButton.setOnClickListener { status.action?.invoke() }
                }
                is StartStepUiStatus.Completed -> {
                    if (status.message != null) {
                        binding.userErrorMessage.isVisible = true
                        binding.userErrorMessage.text = status.message.asString(binding.root.context)
                        binding.userErrorMessage.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))
                    } else {
                        binding.userErrorMessage.isVisible = false
                    }
                    binding.actionButton.isVisible = false
                }
                else -> {
                    binding.userErrorMessage.isVisible = false
                    binding.actionButton.isVisible = false
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StartStepItem>() {
        override fun areItemsTheSame(
            oldItem: StartStepItem,
            newItem: StartStepItem
        ): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(
            oldItem: StartStepItem,
            newItem: StartStepItem
        ): Boolean {
            return oldItem.status == newItem.status
        }
    }
}
