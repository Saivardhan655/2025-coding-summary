package com.example.local_ai_agent

import android.content.Context
import android.util.Log
import com.example.local_ai_agent.data.MyObjectBox
import com.example.local_ai_agent.data.NotificationEmbedding
import java.util.Arrays
import kotlin.math.min

object NotificationEmbeddingDebug {
    fun dumpAllEmbeddings(context: Context) {
        try {
            val boxStore = MyObjectBox.builder().androidContext(context).build()
            val box = boxStore.boxFor(NotificationEmbedding::class.java)
            val all = box.all
            Log.d("OBJBOX_DEBUG", "NotificationEmbedding count=${all.size}")
            for (e in all) {
                val emb = e.embedding
                val len = emb?.size ?: 0
                val firstN = if (emb != null && len > 0) emb.copyOfRange(0, min(5, len)) else FloatArray(0)
                Log.d("OBJBOX_DEBUG", "id=${e.id} notificationId=${e.notificationId} embeddingLen=$len first5=${Arrays.toString(firstN)}")
            }
        } catch (ex: Exception) {
            Log.e("OBJBOX_DEBUG", "failed to dump embeddings", ex)
        }
    }
}
