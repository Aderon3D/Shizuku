package moe.shizuku.manager.permission.ui.authorizedapps

import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.databinding.AppListEmptyBinding
import moe.shizuku.manager.permission.ui.authorizedapps.components.AppViewHolder
import moe.shizuku.manager.permission.ui.authorizedapps.components.EmptyViewHolder
import moe.shizuku.manager.permission.ui.authorizedapps.components.ToggleAllViewHolder

class AppsAdapter(
    private val appViewHolderFactory: AppViewHolder.Factory,
    private val toggleAllViewHolderFactory: ToggleAllViewHolder.Factory
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    init {
        setHasStableIds(true)
    }

    fun updateData(data: List<PackageInfo>) {
        items.clear()
        if (data.isEmpty()) {
            items.add(Any())
        } else {
            items.add(HeaderMarker)
            items.addAll(data)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HeaderMarker -> TYPE_HEADER
            is PackageInfo -> TYPE_APP
            else -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> toggleAllViewHolderFactory.create(parent, { items }) {
                notifyItemRangeChanged(0, itemCount)
            }

            TYPE_APP -> appViewHolderFactory.create(parent) {
                notifyItemRangeChanged(0, itemCount)
            }

            else -> EmptyViewHolder(
                AppListEmptyBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ToggleAllViewHolder -> holder.bind()
            is AppViewHolder -> holder.bind(items[position] as PackageInfo)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        val item = items[position]
        return if (item is PackageInfo) {
            item.packageName.hashCode().toLong()
        } else {
            item.hashCode().toLong()
        }
    }

    object HeaderMarker

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APP = 1
        private const val TYPE_EMPTY = 2
    }
}
