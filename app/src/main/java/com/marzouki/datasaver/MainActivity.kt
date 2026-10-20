package com.marzouki.datasaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.marzouki.datasaver.presentation.AppRoutes
import com.marzouki.datasaver.presentation.DataKeeperApp
import com.marzouki.datasaver.presentation.widget.refreshHomeSurfaces
import com.marzouki.datasaver.ui.theme.DataKeeperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Onglet demandé par un widget ou une tuile, consommé une fois la navigation faite. */
    private var requestedTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) requestedTab = intent.getStringExtra(AppRoutes.EXTRA_TAB)
        setContent {
            DataKeeperTheme {
                DataKeeperApp(requestedTab = requestedTab, onTabRequestHandled = { requestedTab = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(AppRoutes.EXTRA_TAB)?.let { requestedTab = it }
    }

    override fun onStop() {
        super.onStop()
        // Le quota ou les téléchargements ont pu changer : widgets et tuiles se mettent à jour.
        lifecycleScope.launch { refreshHomeSurfaces(applicationContext) }
    }
}
