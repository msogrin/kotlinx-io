/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json

import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlin.test.*

@Serializable
data class IntData(val intV: Int)

@Serializable
data class StringData(val data: String)

enum class SampleEnum { OptionA, OptionB, OptionC }

@Serializable
data class Box<T>(val boxed: T)

@Serializable
sealed class SimpleSealed {
    @Serializable
    public data class SubSealedA(val s: String) : SimpleSealed()

    @Serializable
    public data class SubSealedB(val i: Int) : SimpleSealed()
}


inline fun <reified T : Any> assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = ioJson.plain,
    printResult: Boolean = false
) {
    val string = format.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = format.parse(serializer, string)
    if (printResult) println("[Restored form] $original")
    assertEquals(original, restored)
}

inline fun <reified T : Any> StringFormat.assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    printResult: Boolean = false
) {
    val string = this.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = this.parse(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}