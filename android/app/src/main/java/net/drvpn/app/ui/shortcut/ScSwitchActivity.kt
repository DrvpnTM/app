package net.drvpn.app.ui.shortcut

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import net.drvpn.app.core.CoreServiceManager
import net.drvpn.app.core.LauncherManager
import net.drvpn.app.ui.base.BaseComponentActivity

class ScSwitchActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        LaunchedEffect(Unit) {
            moveTaskToBack(true)
            if (CoreServiceManager.isRunning()) {
                LauncherManager.stopService(this@ScSwitchActivity)
            } else {
                LauncherManager.startServiceFromToggle(this@ScSwitchActivity)
            }
            finish()
        }
    }
}
