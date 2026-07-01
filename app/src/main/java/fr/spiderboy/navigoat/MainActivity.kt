package fr.spiderboy.navigoat

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import fr.spiderboy.navigoat.logic.NfcManager
import fr.spiderboy.navigoat.ui.MainScreen
import fr.spiderboy.navigoat.ui.NavigoViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NavigoViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_LONG).show()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(state = viewModel.uiState) {
                        viewModel.clearState()
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        NfcManager.setupForegroundDispatch(this, nfcAdapter)
    }

    override fun onPause() {
        super.onPause()
        NfcManager.stopForegroundDispatch(this, nfcAdapter)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let {
                viewModel.onTagDiscovered(it)
            }
        }
    }
}
