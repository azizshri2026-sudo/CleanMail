package cleanmail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cleanmail.oauth.OAuthCallbackBus
import cleanmail.oauth.OAuthCallbackResult
import cleanmail.ui.CleanMailNavHost
import cleanmail.ui.theme.CleanMailTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle deep link if app was launched via cleanmail://oauth
        handleOAuthIntent(intent)
        setContent {
            CleanMailTheme {
                CleanMailNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "cleanmail" || uri.host != "oauth") return

        val code     = uri.getQueryParameter("code")     ?: return
        val state    = uri.getQueryParameter("state")    ?: return
        val provider = uri.getQueryParameter("provider") ?: "google"

        OAuthCallbackBus.post(OAuthCallbackResult(code, state, provider))
    }
}
