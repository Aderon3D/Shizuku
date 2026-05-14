package moe.shizuku.manager.core.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.ui.helpers.ThemeHelper
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.AppbarFragmentActivityBinding
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(AppbarFragmentActivityBinding::inflate)
    private lateinit var navController: NavController

    private val themeHelper: ThemeHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        themeHelper.applyTheme(this)
        setContentView(binding.root)

        setupUI()
        observeThemeChanges()
        maybeDeepLinkToSettings(intent)
    }

    private fun setupUI() {
        // Appbar Setup
        binding.appbar.toolbarContainer.applySystemBarsPadding(top = true, start = true, end = true)
        setSupportActionBar(binding.appbar.toolbar)

        // Navigation Setup
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Connect them
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.home_fragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun observeThemeChanges() {
        themeHelper.recreateTrigger
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                recreate()
            }
            .launchIn(lifecycleScope)
    }

    private fun maybeDeepLinkToSettings(intent: Intent?) {
        if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            navController.navigate(R.id.settings_fragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        maybeDeepLinkToSettings(intent)
    }
}
