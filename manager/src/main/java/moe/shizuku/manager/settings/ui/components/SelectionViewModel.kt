package moe.shizuku.manager.settings.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SelectionViewModel : ViewModel() {
    var items: List<SelectionItem<Any>> = emptyList()
    var selectedValue: Any? = null

    private val _results = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val results = _results.asSharedFlow()

    fun select(value: Any) {
        viewModelScope.launch {
            _results.emit(value)
        }
    }
}

data class SelectionItem<out T>(
    val value: T,
    val label: CharSequence,
    val description: CharSequence? = null,
    val isEnabled: Boolean = true
)
