/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.smithy.kotlin.runtime.http.engine

import aws.smithy.kotlin.runtime.http.Url

/**
 * A proxy configuration
 */
sealed class ProxyConfig {
    /**
     * Represents a direct connection or absence of a proxy. Can be used to disable proxy support inferred from
     * environment for example.
     */
    object Direct : ProxyConfig()

    /**
     * HTTP based proxy (with or without user/password auth)
     */
    data class Http(val url: Url) : ProxyConfig() {
        constructor(url: String) : this(Url.parse(url))
    }
}