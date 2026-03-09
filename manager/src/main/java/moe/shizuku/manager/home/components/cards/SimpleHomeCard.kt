package moe.shizuku.manager.home.components.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import moe.shizuku.manager.databinding.SimpleHomeCardBinding

class SimpleHomeCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs), View.OnClickListener {

    private val binding = SimpleHomeCardBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    var title: CharSequence?
        get() = binding.title.text
        set(value) {
            binding.title.text = value
        }

    var summary: CharSequence?
        get() = binding.summary.text
        set(value) {
            if (value.isNullOrEmpty()) {
                binding.summary.isVisible = false
            } else {
                binding.summary.isVisible = true
                binding.summary.text = value
            }
        }

    var icon: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                binding.icon.setImageResource(value)
            }
        }

    var onClickListener: () -> Unit = {}
        set(value) {
            field = value
            binding.root.setOnClickListener(this)
        }

    override fun onClick(v: View) {
        onClickListener()
    }
}
