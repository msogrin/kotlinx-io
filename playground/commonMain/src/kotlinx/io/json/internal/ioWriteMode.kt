/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.io.json.internal

import kotlinx.io.json.*
import kotlinx.serialization.*
import kotlin.jvm.*

enum class ioWriteMode(@JvmField val begin: Char, @JvmField val end: Char) {
    OBJ(BEGIN_OBJ, END_OBJ),
    LIST(BEGIN_LIST, END_LIST),
    MAP(BEGIN_OBJ, END_OBJ),
    POLY_OBJ(BEGIN_LIST, END_LIST);

    @JvmField
    val beginTc: Byte = charToTokenClass(begin)

    @JvmField
    val endTc: Byte = charToTokenClass(end)
}

internal fun ioJson.switchMode(desc: SerialDescriptor): ioWriteMode =
    when (desc.kind) {
        is PolymorphicKind -> ioWriteMode.POLY_OBJ
        StructureKind.LIST -> ioWriteMode.LIST
        StructureKind.MAP -> selectMapMode(desc, {
            ioWriteMode.MAP
        }, { ioWriteMode.LIST })
        else -> ioWriteMode.OBJ
    }

internal inline fun <T, R1 : T, R2 : T> ioJson.selectMapMode(
    mapDescriptor: SerialDescriptor,
    ifMap: () -> R1,
    ifList: () -> R2
): T {
    val keyDescriptor = mapDescriptor.getElementDescriptor(0)
    val keyKind = keyDescriptor.kind
    return if (keyKind is PrimitiveKind || keyKind == UnionKind.ENUM_KIND) {
        ifMap()
    } else if (configuration.allowStructuredMapKeys) {
        ifList()
    } else {
        throw ioJsonMapInvalidKeyKind(keyDescriptor)
    }
}
