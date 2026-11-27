/**
 * NanzBeatles Project (C) 2026
 */

package com.ikyvortex.vortexmusic.extensions

fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        null
    }
