package fr.spiderboy.navigoat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.spiderboy.navigoat.R
import fr.spiderboy.navigoat.models.CardInfo
import fr.spiderboy.navigoat.models.NavigoContract
import fr.spiderboy.navigoat.models.NavigoEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: NavigoUiState,
    onReset: () -> Unit
) {
    val darkPurple = colorResource(id = R.color.purple_dark)
    val lightPurple = colorResource(id = R.color.purple_light)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigoat", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkPurple
                ),
                actions = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.Black)
                    }
                }
            )
        },
        containerColor = lightPurple
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.cardInfo == null) {
                EmptyState(state.statusMessage, state.isLoading)
            } else {
                CardDetails(state.cardInfo)
            }
        }
    }
}

@Composable
fun EmptyState(message: String, isLoading: Boolean) {
    val darkPurple = colorResource(id = R.color.purple_dark)
    val lightPurple = colorResource(id = R.color.purple_light)

    Box(modifier = Modifier.fillMaxSize()) {
        // Fallback Background (Vertical Split)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(darkPurple))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(lightPurple))
        }

        Image(
           painter = painterResource(id = R.drawable.waiting_card),
           contentDescription = null,
           modifier = Modifier.fillMaxSize(),
           contentScale = ContentScale.Crop
        )

        // Center Icon (Goat placeholder if icon not available)
        // Icon(
        //    painter = painterResource(id = R.drawable.unknown),
        //    contentDescription = null,
        //    modifier = Modifier.size(128.dp).align(Alignment.Center),
        //    tint = Color.Black
        // )

        // Status Message Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun CardDetails(card: CardInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CardSectionHeader("Card Information")
            InfoRow("UID", card.uid)
        }

        if (card.contracts.isNotEmpty()) {
            item { CardSectionHeader("Contracts") }
            items(card.contracts) { contract ->
                ContractItem(contract)
            }
        }

        if (card.events.isNotEmpty()) {
            item { CardSectionHeader("Events") }
            items(card.events) { event ->
                EventItem(event)
            }
        }

        if (card.specialEvents.isNotEmpty()) {
            item { CardSectionHeader("Special Events") }
            items(card.specialEvents) { event ->
                EventItem(event)
            }
        }
    }
}

@Composable
fun CardSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.Black,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(value, color = Color.Black)
    }
}

@Composable
fun ContractItem(contract: NavigoContract) {
    val darkPurple = colorResource(id = R.color.purple_dark)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = darkPurple,
            contentColor = Color.Black
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(contract.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(contract.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EventItem(event: NavigoEvent) {
    val darkPurple = colorResource(id = R.color.purple_dark)
    val lightPurple = colorResource(id = R.color.purple_light)
    val context = LocalContext.current

    val logoResId = remember(event.line, event.lineLogo) {
        val logoName = event.lineLogo ?: event.line
        if (logoName.isEmpty()) 0
        else {
            val resName = logoName.lowercase().replace(" ", "_")
            context.resources.getIdentifier(resName, "drawable", context.packageName)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = darkPurple,
            contentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(event.content, style = MaterialTheme.typography.bodyMedium)
            }
            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = event.line,
                    modifier = Modifier.size(32.dp)
                )
            } else if (event.line.isNotEmpty()) {
                Badge(
                    containerColor = lightPurple,
                    contentColor = Color.Black
                ) {
                    Text(event.line)
                }
            }
        }
    }
}
