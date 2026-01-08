package com.chesstastic.mesh

import android.content.Context
import android.content.Intent

class MeshService(private val context: Context) {

    companion object {
        const val ACTION_SEND_MESSAGE = "com.meshtastic.SEND_MESSAGE"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_PORT = "port"
        const val EXTRA_DEST = "dest"
        
        const val CHESS_PORT = 256
        const val BROADCAST_DEST = "!ffffffff"
    }

    fun sendToMesh(message: String, destId: String = BROADCAST_DEST) {
        val intent = Intent(ACTION_SEND_MESSAGE).apply {
            putExtra(EXTRA_PAYLOAD, message)
            putExtra(EXTRA_PORT, CHESS_PORT)
            putExtra(EXTRA_DEST, destId)
            `package` = "com.geeksville.mesh"
        }
        context.sendBroadcast(intent)
    }

    // Backwards compatibility / Helper
    fun sendMove(move: String) {
         sendToMesh(move) // Broadcasts move
    }
    
    fun sendInvite(destId: String) {
        sendToMesh("REQ_GAME", destId)
    }
    
    fun acceptInvite(destId: String) {
        sendToMesh("ACC_GAME", destId)
    }
}
