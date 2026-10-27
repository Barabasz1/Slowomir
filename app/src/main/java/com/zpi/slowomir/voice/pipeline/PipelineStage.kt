package com.zpi.slowomir.voice.pipeline

/**
 * Represents a single transformation, filter, or enrichment step within a processing pipeline.
 *
 * @param I The input data type accepted by this stage.
 * @param O The output data type produced by this stage.
 */
fun interface PipelineStage<I, O> {

    /**
     * Processes input data and returns the resulting output, or `null` to halt pipeline execution.
     *
     * @param input The incoming data unit.
     * @return The transformed output of type [O], or `null` if the pipeline processing should stop.
     */
    suspend fun process(input: I): O?
}
