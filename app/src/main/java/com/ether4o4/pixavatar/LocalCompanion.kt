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
            onProgress(progress.downloadedBytes ?: 0L, progress.totalBytes ?: 0L)
        }
        file.absolutePath
    }

    suspend fun reply(userText: String): String = withContext(Dispatchers.IO) {
        history.addLast("user|${userText.trim()}")
        while (history.size > 8) history.removeFirst()

        // Qwen2.5-Instruct expects its ChatML conversation format. Using the
        // native template keeps the small model from treating the prompt as
        // ordinary text and producing malformed/gibberish replies.
        val prompt = buildString {
            appendLine("<|im_start|>system")
            appendLine("You are PixAvatar, a compact local AI companion on an Android phone.")
            appendLine("Be conversational, direct, friendly, and concise.")
            appendLine("Choose exactly one emotion: neutral, happy, thinking, surprised, or annoyed.")
            appendLine("Start every response with exactly one line in this form: EMOTION=happy")
            appendLine("Then write only the short spoken reply. Do not add labels, markdown, or explanations about the format.")
            appendLine("<|im_end|>")

            history.forEach { turn ->
                val separator = turn.indexOf('|')
                val role = if (separator > 0) turn.substring(0, separator) else "user"
                val text = if (separator > 0) turn.substring(separator + 1) else turn
                appendLine("<|im_start|>$role")
                appendLine(text)
                appendLine("<|im_end|>")
            }
            append("<|im_start|>assistant\n")
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
            history.addLast("assistant|$response")
            while (history.size > 8) history.removeFirst()
        }
    }

    fun close() {
        edge.close()
    }
}
