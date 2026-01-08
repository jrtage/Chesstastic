package com.chesstastic.mesh

import androidx.compose.runtime.mutableStateListOf

data class MeshNode(
    val nodeId: String,
    val longName: String,
    val shortName: String,
    val lastHeard: Long = System.currentTimeMillis()
)

object NodeManager {
    // Observable list for Compose
    val nodes = mutableStateListOf<MeshNode>()

    fun updateNode(nodeId: String, longName: String?, shortName: String?) {
        val existingIndex = nodes.indexOfFirst { it.nodeId == nodeId }
        val newNode = MeshNode(
            nodeId = nodeId,
            longName = longName ?: "Unknown",
            shortName = shortName ?: "?",
            lastHeard = System.currentTimeMillis()
        )

        if (existingIndex >= 0) {
            nodes[existingIndex] = newNode
        } else {
            nodes.add(newNode)
        }
    }
}
