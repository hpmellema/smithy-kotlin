/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.smithy.kotlin.runtime.http.engine.okhttp

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.io.internal.toOkio
import aws.smithy.kotlin.runtime.io.internal.toSdk
import aws.smithy.kotlin.runtime.io.readAll
import aws.smithy.kotlin.runtime.telemetry.logging.trace
import aws.smithy.kotlin.runtime.util.derivedName
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * OkHttp [RequestBody] that reads from [body] channel or source
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalStdlibApi::class)
@InternalApi
public class StreamingRequestBody(
    private val body: HttpBody,
    callContext: CoroutineContext,
) : RequestBody() {
    private val producerJob = Job(callContext[Job])

    private val context: CoroutineContext = callContext +
        producerJob +
        callContext.derivedName("send-request-body") +
        Dispatchers.IO

    init {
        require(body is HttpBody.ChannelContent || body is HttpBody.SourceContent) { "Invalid streaming body $body" }
    }

    override fun contentType(): MediaType? = null
    override fun contentLength(): Long = body.contentLength ?: -1
    override fun isOneShot(): Boolean = body.isOneShot
    override fun isDuplex(): Boolean = body.isDuplex

    override fun writeTo(sink: BufferedSink) {
        try {
            doWriteTo(sink)
        } catch (t: Throwable) {
            when (t) {
                is CancellationException -> {
                    context.trace<StreamingRequestBody> { "request cancelled" }
                    throw t
                }
                is IOException -> throw t
                // wrap all exceptions thrown from inside `okhttp3.RequestBody#writeTo(..)` as an IOException
                // see https://github.com/awslabs/aws-sdk-kotlin/issues/733
                else -> throw IOException(t)
            }
        }
    }

    private fun doWriteTo(sink: BufferedSink) {
        if (isDuplex()) {
            // launch coroutine that writes to sink in the background
            CoroutineScope(context).launch {
                sink.use { transferBody(it) }
            }
        } else {
            // remove the current dispatcher (if it exists) and use the internal
            // runBlocking dispatcher that blocks the *current* thread
            val blockingContext = context.minusKey(CoroutineDispatcher)

            // Non-duplex (aka "normal") requests MUST write all of their request body
            // before this function returns. Requests are given a background thread to
            // do this work in, and it is safe and expected to block.
            // see: https://square.github.io/okhttp/4.x/okhttp/okhttp3/-request-body/is-duplex/
            runBlocking(blockingContext) {
                transferBody(sink)
            }
        }
    }

    private suspend fun transferBody(sink: BufferedSink) = withJob(producerJob) {
        when (body) {
            is HttpBody.ChannelContent -> {
                val chan = body.readFrom()
                val sdkSink = sink.toSdk()
                chan.readAll(sdkSink)
            }

            is HttpBody.SourceContent -> {
                val source = body.readFrom()
                source.toOkio().use {
                    sink.writeAll(it)
                }
            }
            // should never hit - all other body types are handled elsewhere
            else -> error("unexpected HttpBody type $body")
        }
    }
}

/**
 * Completes the given job when the block returns calling either `complete()` when the block runs
 * successfully or `completeExceptionally()` on exception.
 * @return the result of calling [block]
 */
private inline fun <T> withJob(job: CompletableJob, block: () -> T): T {
    try {
        return block().also { job.complete() }
    } catch (t: Throwable) {
        job.completeExceptionally(t)
        throw t
    }
}
