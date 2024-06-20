/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.smithy.kotlin.runtime.http.test.util

import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

// FIXME add engines to test
internal actual fun engineFactories(): List<TestEngineFactory> =
    listOf()

// FIXME add servers to test
internal actual val testServers = mapOf<ServerType, Url>()

internal actual fun runBlockingTest(
    context: CoroutineContext,
    timeout: Duration?,
    block: suspend CoroutineScope.() -> Unit,
) {
    runBlocking(context) {
        if (timeout != null) {
            withTimeout(timeout) {
                block()
            }
        } else {
            block()
        }
    }
}
