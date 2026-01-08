package com.chesstastic.mesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MeshBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RECEIVE_MESSAGE = "com.meshtastic.RECEIVE_MESSAGE"
        const val ACTION_NODE_UPDATE = "com.meshtastic.NODE_UPDATE"
        
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_PORT = "port"
        
        // Node Update Extras (Best guess based on standard Android conventions + User hint)
        const val EXTRA_NODE_ID = "id" // or "nodeId"? User said "What you get: The Node ID..."
        const val EXTRA_LONG_NAME = "longName"
        const val EXTRA_SHORT_NAME = "shortName"
        
        // Callbacks
        var onMoveReceived: ((String) -> Unit)? = null
        var onGameInviteReceived: ((from: String) -> Unit)? = null
        var onGameAcceptedReceived: ((from: String) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NODE_UPDATE -> {
                val id = intent.getStringExtra(EXTRA_NODE_ID) ?: intent.getStringExtra("nodeId")
                val longName = intent.getStringExtra(EXTRA_LONG_NAME)
                val shortName = intent.getStringExtra(EXTRA_SHORT_NAME)
                
                if (id != null) {
                    NodeManager.updateNode(id, longName, shortName)
                }
            }
            ACTION_RECEIVE_MESSAGE -> {
                val port = intent.getIntExtra(EXTRA_PORT, -1)
                if (port == MeshService.CHESS_PORT) {
                    val payload = intent.getStringExtra(EXTRA_PAYLOAD) 
                        ?: intent.getByteArrayExtra(EXTRA_PAYLOAD)?.let { String(it) }
                    
                    if (payload != null) {
                        // Check for protocol messages
                        when {
                            payload == "REQ_GAME" -> {
                                val senderId = intent.getStringExtra("from") ?: "Unknown" // Assuming 'from' extra exists
                                onGameInviteReceived?.invoke(senderId)
                            }
                            payload == "ACC_GAME" -> {
                                val senderId = intent.getStringExtra("from") ?: "Unknown"
                                onGameAcceptedReceived?.invoke(senderId)
                            }
                            else -> {
                                // Assume it's a move
                                onMoveReceived?.invoke(payload)
                            }
                        }
                    }
                }
            }
        }
    }
}
