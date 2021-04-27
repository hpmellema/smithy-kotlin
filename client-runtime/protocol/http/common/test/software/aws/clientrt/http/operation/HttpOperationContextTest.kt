/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.clientrt.http.operation

import software.aws.clientrt.client.SdkClientOption
import software.aws.clientrt.util.get
import kotlin.test.*

class HttpOperationContextTest {

    @Test
    fun testBuilder() {
        val op = HttpOperationContext.build {
            service = "test"
            operationName = "operation"
            expectedHttpStatus = 418
        }

        assertEquals("test", op[(SdkClientOption.ServiceName)])
        assertEquals("operation", op[SdkClientOption.OperationName])
        assertEquals(418, op[HttpOperationContext.ExpectedHttpStatus])
    }

    @Test
    fun testMissingRequiredProperties() {
        val ex = assertFailsWith<IllegalArgumentException> {
            HttpOperationContext.build {
                service = "test"
            }
        }

        assertTrue(ex.message!!.contains("OperationName is a required property"))
    }
}