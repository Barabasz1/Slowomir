package com.zpi.slowomir.overlay.pipeline

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.mediator.CommandMediator

/**
 * Pipeline that passes input data sequentially through a set of [PipelineStage]s,
 * extracts resulting [Command] instances, and dispatches them to a [CommandMediator].
 *
 * @param TInput The initial input type entering the pipeline.
 */
class CommandPipeline<TInput> private constructor(
    private val stages: List<PipelineStage<Any?, Any?>>,
    private val mediator: CommandMediator
) {

    /**
     * Processes input through all pipeline stages and dispatches produced commands.
     *
     * @param input The raw input to be processed by the pipeline.
     */
    suspend fun execute(input: TInput) {
        val result = processStages(input) ?: return
        dispatchResult(result)
    }

    private suspend fun processStages(input: TInput): Any? {
        var current: Any? = input
        for (stage in stages) {
            if (current == null) return null
            current = stage.process(current)
        }
        return current
    }

    private suspend fun dispatchResult(result: Any) {
        when (result) {
            is Command -> {
                mediator.dispatch(result)
            }
            is Iterable<*> -> {
                result.filterIsInstance<Command>().forEach { command ->
                    mediator.dispatch(command)
                }
            }
        }
    }

    /**
     * Builder to construct a type-safe [CommandPipeline].
     */
    class Builder<TInput>(private val mediator: CommandMediator) {
        private val stages = mutableListOf<PipelineStage<Any?, Any?>>()

        @Suppress("UNCHECKED_CAST")
        fun <TNext> addStage(stage: PipelineStage<*, TNext>): Builder<TInput> {
            stages.add(stage as PipelineStage<Any?, Any?>)
            return this
        }

        fun build(): CommandPipeline<TInput> {
            return CommandPipeline(stages, mediator)
        }
    }
}
