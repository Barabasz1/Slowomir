package com.zpi.slowomir.voice.pipeline

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.voice.mediator.CommandMediator

/**
 * Pipeline that passes input data sequentially through a set of [PipelineStage]s,
 * extracts resulting [Command] instances, and dispatches them to a [CommandMediator].
 *
 */
/**
 * Pipeline that processes [TInput] into [TOutput] and dispatches any extracted [Command]s.
 *
 * @param TInput The initial input type.
 * @param TOutput The output type.
 */
class Pipeline<TInput, TOutput> private constructor(
    private val stages: List<PipelineStage<Any?, Any?>>,
) {

    suspend fun process(input: TInput): TOutput? {
        val result = processStages(input) ?: return null
        @Suppress("UNCHECKED_CAST")
        return result as TOutput
    }

    private suspend fun processStages(input: TInput): Any? {
        var current: Any? = input
        for (stage in stages) {
            if (current == null) return null
            current = stage.process(current)
        }
        return current
    }

    class Builder<TInput, TCurrent> private constructor(
        private val stages: MutableList<PipelineStage<Any?, Any?>>
    ) {
        companion object {
            fun <TInput> create(): Builder<TInput, TInput> {
                return Builder( mutableListOf())
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <TNext> addStage(stage: PipelineStage<TCurrent, TNext>): Builder<TInput, TNext> {
            stages.add(stage as PipelineStage<Any?, Any?>)
            return Builder( stages)
        }

        fun build(): Pipeline<TInput, TCurrent> {
            return Pipeline(stages)
        }
    }
}
