package com.zpi.slowomir.voice.intent

import android.content.Context
import android.util.Log
import com.zpi.slowomir.ml.UnigramTokenizer
import com.zpi.slowomir.ml.ModelOutput
import com.zpi.slowomir.ml.OnnxTextModel
import com.zpi.slowomir.ml.argmax
import com.zpi.slowomir.ml.decodeBioSpans
import com.zpi.slowomir.ml.softmax
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Resolves the [CommandIdentifier] and [CommandArgument] using encoder model provided by [OnnxTextModel].
 */
class CommandIntentResolverManager @Inject constructor(
    @ApplicationContext context: Context,
) : CommandIntentResolver {

    private val model = OnnxTextModel(
        context = context,
        modelAsset = "$ASSET_DIR/model.int8.onnx",
        tokenizer = UnigramTokenizer(context, "$ASSET_DIR/tokenizer.json"),
        maxLength = MAX_LENGTH,
    )

    override suspend fun initialize(): Result<Unit> {
        CommandIntentLabels.INTENTS
        return model.initialize()
    }

    override suspend fun resolve(text: String): IntentResolution {
        val output = try {
            model.infer(text)
        } catch (t: Throwable) {
            Log.e(TAG, "Intent inference failed", t)
            return IntentResolution.Error(IntentResolverError.INFERENCE_FAILED)
        } ?: return IntentResolution.Error(IntentResolverError.NOT_INITIALIZED)

        val logits = output.sequenceLogits
            ?: return IntentResolution.Error(IntentResolverError.INFERENCE_FAILED)

        val probs = softmax(logits)
        val best = argmax(probs)
        val identifier = CommandIntentLabels.INTENTS.getOrNull(best)
        if (identifier == null || probs[best] < THRESHOLD) return IntentResolution.Unrecognized

        return IntentResolution.Resolved(identifier, probs[best], extractArgument(text, output))
    }

    override fun release() = model.release()

    private fun extractArgument(text: String, output: ModelOutput): CommandArgument? {
        val tokenLogits = output.tokenLogits ?: return null
        val spans = decodeBioSpans(text, output.encoding, tokenLogits, CommandIntentLabels.TAGS)

        val numbers = spans.filter { it.label == "NUMBER" }
            .mapNotNull { text.substring(it.range).filter(Char::isDigit).toIntOrNull() }
        val names = spans.filter { it.label == "NAME" }
            .map { text.substring(it.range).trim() }
            .filter { it.isNotEmpty() }

        return when {
            numbers.size >= 2 -> CommandArgument.TwoNumbers(numbers[0], numbers[1])
            numbers.size == 1 -> CommandArgument.Number(numbers[0])
            names.isNotEmpty() -> CommandArgument.Text(names.joinToString(" "))
            else -> null
        }
    }

    private companion object {
        const val TAG = "CommandIntentResolver"
        const val ASSET_DIR = "sdadas-polish-distilroberta-cmd"
        const val MAX_LENGTH = 64
        const val THRESHOLD = 0.6f
    }
}

/**
 * Output layout of the intent model. Must stay in sync with `labels.json` and with [CommandIdentifier].
 */
internal object CommandIntentLabels {

    private val ALIASES = mapOf(
        "HIDE_KEYBOARD" to "KEYBOARD_HIDE",
        "SHOW_KEYBOARD" to "KEYBOARD_SHOW",
    )

    /** Class order from the training config (alphabetical). Do not reorder. */
    private val LABELS = listOf(
        "CELL_DIVIDE", "CELL_LONG_PRESS", "CELL_PRESS", "CELL_SELECT", "CELL_SWIPE",
        "CELL_UNDIVIDE", "GRID_HIDE", "GRID_RESET", "GRID_SHOW", "HIDE_KEYBOARD",
        "MACRO_FINISH", "MACRO_PLAY", "MACRO_RECORD", "MEDIA_NEXT_TRACK", "MEDIA_PAUSE",
        "MEDIA_PLAY", "MEDIA_PREVIOUS_TRACK", "MEDIA_SKIP_BACKWARD", "MEDIA_SKIP_FORWARD",
        "NAVIGATE_APP_OPEN", "NAVIGATE_BACK", "NAVIGATE_HOME", "NAVIGATE_RECENT",
        "NOTIFICATIONS", "OVERLAY_START", "OVERLAY_STOP", "PHONE_CALL", "PHONE_DECLINE",
        "PHONE_END_CALL", "PHONE_PICK_UP", "PHONE_SPEAKER", "PREDICTION_CONFIRM",
        "QUICK_SETTINGS", "SHOW_KEYBOARD", "SMS_WRITE", "SWIPE_DOWN", "SWIPE_LEFT",
        "SWIPE_RIGHT", "SWIPE_UP", "TEXT_CONFIRM", "TEXT_CURSOR_BACKWARD",
        "TEXT_CURSOR_FORWARD", "TEXT_CURSOR_NEXT_WORD", "TEXT_CURSOR_PREVIOUS_WORD",
        "TEXT_DELETE_CHARACTER", "TEXT_DELETE_WORD", "TEXT_ENTER", "VOLUME_DOWN",
        "VOLUME_UP", "ZOOM_IN", "ZOOM_OUT",
    )

    val INTENTS: List<CommandIdentifier> = run {
        val byName = CommandIdentifier.entries.associateBy { it.name }
        val missing = LABELS.filter { byName[ALIASES[it] ?: it] == null }
        check(missing.isEmpty()) {
            "Intent model labels without a matching CommandIdentifier: $missing. " +
                    "Add an alias in CommandIntentLabels.ALIASES or update the model/enum so they match."
        }
        LABELS.map { byName.getValue(ALIASES[it] ?: it) }
    }

    val TAGS = listOf("B-NAME", "B-NUMBER", "I-NAME", "I-NUMBER", "O")
}
