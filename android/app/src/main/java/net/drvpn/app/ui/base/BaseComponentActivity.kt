package net.drvpn.app.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import net.drvpn.app.handler.AppLocaleManager
import net.drvpn.app.ui.compose.AppTheme

abstract class BaseComponentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                ScreenContent()
            }
        }
    }

    @Composable
    protected abstract fun ScreenContent()
}