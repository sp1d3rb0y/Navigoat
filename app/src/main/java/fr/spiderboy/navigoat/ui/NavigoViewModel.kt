package fr.spiderboy.navigoat.ui

import android.app.Application
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.spiderboy.navigoat.R
import fr.spiderboy.navigoat.logic.ApduUtils
import fr.spiderboy.navigoat.logic.NavigoParser
import fr.spiderboy.navigoat.logic.NfcManager
import fr.spiderboy.navigoat.models.CardInfo
import kotlinx.coroutines.launch

class NavigoViewModel(application: Application) : AndroidViewModel(application) {
    private val nfcManager = NfcManager(NfcAdapter.getDefaultAdapter(application))
    
    var uiState by mutableStateOf(NavigoUiState())
        private set

    fun onTagDiscovered(tag: Tag) {
        val parser = NavigoParser(
            getApplication<Application>().resources.getXml(R.xml.card_struct),
            getApplication<Application>().resources.getXml(R.xml.stations)
        )
        
        uiState = uiState.copy(isLoading = true, statusMessage = "Reading card...")
        
        viewModelScope.launch {
            val dump = nfcManager.readCard(tag, parser)
            uiState = uiState.copy(
                isLoading = false,
                cardInfo = CardInfo(
                    uid = ApduUtils.toHexString(tag.id),
                    events = parser.getEvents(),
                    contracts = parser.getContracts(),
                    specialEvents = parser.getSpecialEvents(),
                    rawDump = dump
                ),
                statusMessage = "Card read successfully"
            )
        }
    }

    fun clearState() {
        uiState = NavigoUiState()
    }
}

data class NavigoUiState(
    val isLoading: Boolean = false,
    val cardInfo: CardInfo? = null,
    val statusMessage: String = "Waiting for card..."
)
