package dev.bright.vb6serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal open class ConstByteSizeCollectionSerializationStrategy<T>(
    private val inner: SerializationStrategy<T>,
    private val collectionMaxSize: Int,
    private val collectionActualSize: Int,
) : SerializationStrategy<T>, ConstByteSizeSerializationStrategy<T> {
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        val binaryEncoder = encoder.requireHasOutputEncoder()
        binaryEncoder.output.addPaddingWithRatio(collectionActualSize, collectionMaxSize) {
            inner.serialize(encoder, value)
        }
    }
}

open class ConstByteSizeStringSerializer(
    private val byteSize: Int,
    private val inner: KSerializer<String> = String.serializer()
) : KSerializer<String> {
    override val descriptor: SerialDescriptor get() = inner.descriptor

    override fun deserialize(decoder: Decoder): String {
        val binaryDecoder = decoder.requireBinaryDecoderBase()
        return binaryDecoder.input.skipBytesUpToAfterReading(byteSize) {
            binaryDecoder.decodeStringWithLength(byteSize)
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        val binaryEncoder = encoder.requireHasOutputEncoder()
        binaryEncoder.output.addPaddingAfterBytesWritten(byteSize) {
            inner.serialize(encoder, value)
        }
    }
}

internal interface ConstByteSizeSerializationStrategy<T> : SerializationStrategy<T>
internal interface ConstByteSizeDeserializationStrategy<T> : DeserializationStrategy<T>
internal interface ConstByteSizeKSerializer<T> : KSerializer<T>, ConstByteSizeSerializationStrategy<T>,
    ConstByteSizeDeserializationStrategy<T>

open class ConstByteSizeCollectionKSerializer<T>(
    private val inner: KSerializer<T>, private val elementByteSize: Int, private val collectionMaxSize: Int
) : KSerializer<T>, ConstByteSizeKSerializer<T> {

    override val descriptor: SerialDescriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        val binaryEncoder = encoder.requireHasOutputEncoder()
        binaryEncoder.output.addPaddingAfterBytesWritten(collectionMaxSize * elementByteSize) {
            inner.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): T {
        val binaryDecoder = decoder.requireBinaryDecoderBase()
        return binaryDecoder.input.skipBytesUpToAfterReading(collectionMaxSize * elementByteSize) {
            val limitedInput = binaryDecoder.input.withBytesLimitedTo(collectionMaxSize * elementByteSize)
            inner.deserialize(BinaryDecoder(limitedInput, binaryDecoder.serializersModule))
        }
    }
}
