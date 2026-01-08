package com.example.local_ai_agent.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class NotificationEmbedding(
    @Id
    var id: Long = 0,
    var notificationId: String = "",
    var text: String = "",
    var timestamp: Long = 0L,
    var embedding: FloatArray = FloatArray(384)
)