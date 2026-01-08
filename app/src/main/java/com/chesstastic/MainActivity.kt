package com.chesstastic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chesstastic.game.ChessGame
import com.chesstastic.game.Square
import com.chesstastic.game.toSquare
import com.chesstastic.mesh.MeshBroadcastReceiver
import com.chesstastic.mesh.MeshService
import com.chesstastic.ui.ChessBoard
import com.chesstastic.ui.theme.ChesstasticTheme

class MainActivity : ComponentActivity() {
    private val game = ChessGame()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Note: Callbacks are attached in Composable for state access
        
        setContent {
            ChesstasticTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(game = game)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        MeshBroadcastReceiver.onMoveReceived = null
        MeshBroadcastReceiver.onGameInviteReceived = null
        MeshBroadcastReceiver.onGameAcceptedReceived = null
    }
}

@Composable
fun GameScreen(game: ChessGame) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val meshService = remember { MeshService(context) }
    
    // UI State
    val refreshTrigger = remember { mutableStateOf(false) }
    val manualMoveText = remember { mutableStateOf("") }
    
    // Dialog States
    val showPlayerList = remember { mutableStateOf(false) }
    val incomingInviteFrom = remember { mutableStateOf<String?>(null) }
    
    // Listeners
    androidx.compose.runtime.DisposableEffect(Unit) {
        val originalMove = MeshBroadcastReceiver.onMoveReceived
        val originalInvite = MeshBroadcastReceiver.onGameInviteReceived
        val originalAccept = MeshBroadcastReceiver.onGameAcceptedReceived
        
        MeshBroadcastReceiver.onMoveReceived = { moveStr ->
            if (moveStr.length == 4) {
                // ... same move logic ...
                 val f = moveStr.substring(0,2).toSquare()
                 val t = moveStr.substring(2,4).toSquare()
                 if (f != null && t != null) {
                      if (game.makeMove(f, t)) refreshTrigger.value = !refreshTrigger.value
                 }
            }
        }
        
        MeshBroadcastReceiver.onGameInviteReceived = { fromId ->
             incomingInviteFrom.value = fromId
        }
        
        MeshBroadcastReceiver.onGameAcceptedReceived = { fromId ->
             // Handle acceptance (Start new game?)
             game.resetBoard()
             refreshTrigger.value = !refreshTrigger.value
             android.widget.Toast.makeText(context, "Game Accepted by $fromId! White to move.", android.widget.Toast.LENGTH_LONG).show()
        }

        onDispose {
            MeshBroadcastReceiver.onMoveReceived = originalMove
            MeshBroadcastReceiver.onGameInviteReceived = originalInvite
            MeshBroadcastReceiver.onGameAcceptedReceived = originalAccept
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar Area
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Turn: ${game.turn}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Button(onClick = { showPlayerList.value = true }) {
                Text("Players")
            }
        }

        ChessBoard(
            game = game,
            onMove = { from, to ->
                if (game.makeMove(from, to)) {
                    refreshTrigger.value = !refreshTrigger.value
                    meshService.sendMove("${from}${to}")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
        
        // Debug Tools (Collapsed by default? Keeping simple)
        androidx.compose.material3.OutlinedTextField(
            value = manualMoveText.value,
            onValueChange = { manualMoveText.value = it },
            label = { Text("Receive Move (Debug)") }
        )
        androidx.compose.material3.Button(onClick = {
            // ... debug logic ...
            if (manualMoveText.value.length == 4) {
                 val text = manualMoveText.value
                 val f = text.substring(0,2).toSquare()
                 val t = text.substring(2,4).toSquare()
                 if (f != null && t != null) {
                     game.makeMove(f, t)
                     refreshTrigger.value = !refreshTrigger.value
                 }
            }
        }) {
            Text("Simulate Receive")
        }
    }
    
    // Player List Dialog
    if (showPlayerList.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPlayerList.value = false },
            title = { Text("Available Players") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                     items(com.chesstastic.mesh.NodeManager.nodes.size) { index ->
                         val node = com.chesstastic.mesh.NodeManager.nodes[index]
                         androidx.compose.material3.ListItem(
                             headlineContent = { Text(node.longName) },
                             supportingContent = { Text(node.nodeId) },
                             trailingContent = {
                                 androidx.compose.material3.Button(onClick = {
                                     meshService.sendInvite(node.nodeId)
                                     showPlayerList.value = false
                                     android.widget.Toast.makeText(context, "Invite Sent!", android.widget.Toast.LENGTH_SHORT).show()
                                 }) {
                                     Text("Invite")
                                 }
                             }
                         )
                     }
                     if (com.chesstastic.mesh.NodeManager.nodes.isEmpty()) {
                         item { Text("No nodes found yet. Waiting for Node Updates...") }
                     }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPlayerList.value = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Invite Received Dialog
    incomingInviteFrom.value?.let { fromId ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { incomingInviteFrom.value = null },
            title = { Text("Game Invite!") },
            text = { Text("Player $fromId wants to play chess.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    meshService.acceptInvite(fromId)
                    game.resetBoard()
                    refreshTrigger.value = !refreshTrigger.value
                    incomingInviteFrom.value = null
                    android.widget.Toast.makeText(context, "Accepted! Game On.", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { incomingInviteFrom.value = null }) {
                    Text("Decline")
                }
            }
        )
    }
}
