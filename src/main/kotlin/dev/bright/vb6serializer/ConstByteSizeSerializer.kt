@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package dev.bright.vb6serializer

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.AbstractCollectionSerializer

internal open class ConstByteSizeCollectionSerializationStrategy<T>(
    private val inner: SerializationStrategy<T>,
    private val collectionMaxSize: Int,
    private val collectionActualSize: Int,
) : SerializationStrategy<T>, ConstByteSizeSerializationStrategy<T> {
    override val totalByteSize: Int
        get() = TODO("Not yet implemented")

    override val descriptor: SerialDescriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        val binaryEncoder = encoder.requireBinaryEncoder()
        binaryEncoder.output.addPaddingWithRatio(collectionActualSize, collectionMaxSize) {
            val elementDescriptor = inner.descriptor.getElementDescriptor(0)
            if (elementDescriptor.kind == StructureKind.LIST) {
                val serialized =
                    serializedBytesOf(binaryEncoder.serializersModule, binaryEncoder.configuration, inner, value)
                @Suppress("UNCHECKED_CAST") val actualSerializer = inner as AbstractCollectionSerializer<T, *, *>
                val elementSerializer = actualSerializer.elementSerializer() as ConstByteSizeCollectionKSerializer<*>
                val rows = collectionActualSize
                val cols = elementSerializer.collectionMaxSize
                transposeInPlace(serialized, rows, cols, elementSerializer.elementByteSize)
                binaryEncoder.output.write(serialized)
            } else {
                inner.serialize(encoder, value)
            }
        }
    }
}

open class ConstByteSizeStringSerializer(
    private val byteSize: Int,
    private val inner: KSerializer<String> = String.serializer()
) : KSerializer<String>, ConstByteSizeSerializationStrategy<String> {
    override val totalByteSize get() = byteSize
    override val descriptor: SerialDescriptor get() = inner.descriptor

    override fun deserialize(decoder: Decoder): String {
        val binaryDecoder = decoder.requireBinaryDecoderBase()
        return binaryDecoder.input.skipBytesUpToAfterReading(byteSize) {
            binaryDecoder.decodeStringWithFixedByteSize(byteSize)
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        val binaryEncoder = encoder.requireHasOutputEncoder()
        binaryEncoder.output.addPaddingAfterBytesWritten(byteSize) {
            inner.serialize(encoder, value)
        }
    }
}

internal interface ConstByteSizeSerializationStrategy<T> : SerializationStrategy<T> {
    val totalByteSize: Int
}

internal interface ConstByteSizeDeserializationStrategy<T> : DeserializationStrategy<T> {
    val totalByteSize: Int
}

internal interface ConstByteSizeCollectionDeserializationStrategy<T> : ConstByteSizeDeserializationStrategy<T> {
    val elementByteSize: Int
    val collectionMaxSize: Int
}
internal interface ConstByteSizeKSerializer<T> : KSerializer<T>,
    ConstByteSizeSerializationStrategy<T>,
    ConstByteSizeDeserializationStrategy<T>

open class ConstByteSizeCollectionKSerializer<T>(
    private val inner: KSerializer<T>, override val elementByteSize: Int, override val collectionMaxSize: Int
) : KSerializer<T>, ConstByteSizeKSerializer<T>, ConstByteSizeCollectionDeserializationStrategy<T> {
    override val totalByteSize get() = elementByteSize * collectionMaxSize
    override val descriptor: SerialDescriptor get() = inner.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        val binaryEncoder = encoder.requireHasOutputEncoder()
        binaryEncoder.output.addPaddingAfterBytesWritten(totalByteSize) {
            inner.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        val binaryDecoder = decoder.requireBinaryDecoderBase()
        return binaryDecoder.input.skipBytesUpToAfterReading(totalByteSize) {
            val limitedInput = binaryDecoder.input.withBytesLimitedTo(totalByteSize)
            inner.deserialize(BinaryDecoder(limitedInput, binaryDecoder.configuration, binaryDecoder.serializersModule))
        }
    }
}
