package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.listElementByteSize(elementSize: Size? = null): Int {
    require(kind == StructureKind.LIST) { "$this must have LIST kind got $kind" }
    return when {
        serialName == "kotlin.ByteArray" -> Byte.SIZE_BYTES
        serialName == "kotlin.ShortArray" -> Short.SIZE_BYTES
        serialName == "kotlin.IntArray" -> Int.SIZE_BYTES
        serialName == "kotlin.LongArray" -> Long.SIZE_BYTES
        serialName == "kotlin.FloatArray" -> Float.SIZE_BYTES
        serialName == "kotlin.DoubleArray" -> Double.SIZE_BYTES
        serialName == "kotlin.UByteArray" -> Byte.SIZE_BYTES
        serialName == "kotlin.UShortArray" -> Short.SIZE_BYTES
        serialName == "kotlin.UIntArray" -> Int.SIZE_BYTES
        serialName == "kotlin.ULongArray" -> Long.SIZE_BYTES
        javaClass.superclass.name == "kotlinx.serialization.internal.ListLikeDescriptor" -> {
            val elementDescriptor = listLikeElementDescriptor()
            return elementDescriptor.byteSize(elementSize)
        }

        else -> {
            throw IllegalArgumentException("Unsupported type $this")
        }
    }
}

private fun SerialDescriptor.listLikeElementDescriptor() =
    javaClass.getMethod("getElementDescriptor").invoke(this) as SerialDescriptor

private fun SerialDescriptor.byteSize(elementSize: Size?): Int {
    when {
        javaClass.name == "kotlinx.serialization.internal.PrimitiveSerialDescriptor" -> {
            val primitiveKind = primitiveSerialDescriptorKind()
            return when (primitiveKind) {
                PrimitiveKind.BOOLEAN -> 1
                PrimitiveKind.BYTE -> Byte.SIZE_BYTES
                PrimitiveKind.CHAR -> Char.SIZE_BYTES
                PrimitiveKind.DOUBLE -> Double.SIZE_BYTES
                PrimitiveKind.FLOAT -> Float.SIZE_BYTES
                PrimitiveKind.INT -> Int.SIZE_BYTES
                PrimitiveKind.LONG -> Long.SIZE_BYTES
                PrimitiveKind.SHORT -> Short.SIZE_BYTES
                PrimitiveKind.STRING -> {
                    elementSize?.length ?: throw IllegalArgumentException("Could not determine size of $this since there's no @${Size::class} annotation")
                }
            }
        }

        else -> {
            throw IllegalArgumentException("Unsupported type $this")
        }
    }
}

private fun SerialDescriptor.primitiveSerialDescriptorKind() =
    javaClass.getMethod("getKind").invoke(this) as PrimitiveKind
