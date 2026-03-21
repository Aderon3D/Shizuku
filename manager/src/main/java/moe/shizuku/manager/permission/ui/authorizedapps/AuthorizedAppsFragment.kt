package moe.shizuku.manager.permission.ui.authorizedapps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.databinding.AuthorizedAppsFragmentBinding
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import java.util.Objects

class AuthorizedAppsFragment : Fragment(R.layout.authorized_apps_fragment) {

    private val viewModel: AuthorizedAppsViewModel by viewModels()
    private val adapter = AppsAdapter()

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isDead() && !isStateSaved) {
            findNavController().popBackStack()
        }
    }

    private val binding by viewBinding(AuthorizedAppsFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!ShizukuStateMachine.isRunning()) {
            findNavController().popBackStack()
            return
        }

        viewModel.packages.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    adapter.updateData(it.data)
                }

                Status.ERROR -> {
                    if (isAdded) {
                        findNavController().popBackStack()
                        val tr = it.error
                        toast(Objects.toString(tr, "unknown"))
                        tr.printStackTrace()
                    }
                }

                Status.LOADING -> {

                }
            }
        }
        viewModel.load()

        val recyclerView = binding.list
        recyclerView.adapter = adapter

        recyclerView.applySystemBarsPadding(bottom = true)

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                viewModel.load(true)
            }
        })

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onDestroyView() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
