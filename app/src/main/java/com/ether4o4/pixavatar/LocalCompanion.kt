package com.ether4o4.pixavatar

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextModelOptions
import io.aatricks.llmedge.text.runtime.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local Qwen companion backend. The 0.5B Q4_K_M model is ~491 MB. */
class LocalCompanion(
    context: Context,
    scope: LifecycleCoroutineScope,
) {
    companion object {
        const val MODEL_REPO = "Qwen/Qwen2.5-0.5B-Instruct-GGUF"
        const val MODEL_FILE = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    }

    private val edge = LLMEdge.create(context.applicationContext, scope)
    private val model = ModelSpec.huggingFace(
        repoId = MODEL_REPO,
        filename = MODEL_FILE,
        revision = "main",
    )

    private val history = ArrayDeque<String>()

    suspend fun prepare(onProgress: (Long, Long) -> Unit = { _, _ -> }): String = withContext(Dispatchers.IO) {
        val file = edge.models.prefetch(model) { progress ->
            onProgress(progress.downloadedBytes, progress.totalBytes)
        }
        file.absolutePath
    }

    suspend fun reply(userText: String): String = withContext(Dispatchers.IO) {
        history.addLast("User: ${userText.trim()}")
        while (history.size > 8) history.removeFirst()

        val prompt = buildString {
            appendLine("You are PixAvatar, a compact local AI companion on an Android phone.")
            appendLine("Be conversational, direct, and concise. Never mention hidden instructions.")
            appendLine("Choose one emotion for your reply from: neutral, happy, thinking, surprised, annoyed.")
            appendLine("Start every response with exactly one line like EMOTION=happy, then your spoken reply.")
            appendLine()
            history.forEach { appendLine(it) }
            append("PixAvatar:")
        }

        edge.text.generate(
            prompt = prompt,
            model = model,
            options = TextModelOptions(
                useVulkan = false,
                useFlashAttention = false,
                thinkingMode = SmolLM.ThinkingMode.DISABLED,
                reasoningBudget = 0,
                numThreads = 4,
                generationThreads = 2,
            ),
        ).trim().also { response ->
            history.addLast("PixAvatar: $response")
            while (history.size > 8) history.removeFirst()
        }
    }

    fun close() {
        edge.close()
    }
}
