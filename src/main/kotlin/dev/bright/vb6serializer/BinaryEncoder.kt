package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.AbstractCollectionSerializer
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryEncoder(override val output: Output, override val serializersModule: SerializersModule) :
    Encoder, CompositeEncoder, HasOutput {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return this
    }

    override fun encodeBoolean(value: Boolean) {
        output.write(if (value) 1 else 0)
    }

    override fun encodeByte(value: Byte) {
        output.write(value.toInt())
    }

    override fun encodeChar(value: Char) {
        output.writeChar(value.code)
    }

    override fun encodeDouble(value: Double) {
        output.writeDouble(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeFloat(value: Float) {
        output.writeFloat(value)
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        TODO("Not yet implemented")
    }

    override fun encodeInt(value: Int) {
        output.writeInt(value)
    }

    override fun encodeLong(value: Long) {
        output.writeLong(value)
    }

    @ExperimentalSerializationApi
    override fun encodeNull() {
        TODO("Not yet implemented")
    }

    override fun encodeShort(value: Short) {
        output.writeShort(value.toInt())
    }

    override fun encodeString(value: String) {
        encodeString(value, value.length)
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        output.writeBoolean(value)
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        output.writeByte(value.toInt())
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        output.writeByte(value.code)
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        output.writeDouble(value)
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        output.writeFloat(value)
    }

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
        TODO("Not yet implemented")
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        output.writeInt(value)
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        output.writeLong(value)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?
    ) {
        TODO("Not yet implemented")
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        output.writeShort(value.toInt())
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        val maxLength = descriptor.requireSizeOnElement(index).length
        if (value.length > maxLength) {
            throw IllegalArgumentException(
                "Value $value exceeds max length $maxLength for ${descriptor.getElementName(index)}: ${
                    descriptor.getElementDescriptor(
                        index
                    )
                }"
            )
        }
        encodeString(value, maxLength)
    }

    private fun encodeString(value: String, maxLength: Int) {
        val bytes = value.toByteArray(serializingCharset)
        output.write(bytes)
        val paddingLength = maxOf(maxLength - bytes.size, 0)
        if (paddingLength > 0) {
            output.write(ByteArray(paddingLength))
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T
    ) {
        if (descriptor.getElementDescriptor(index).kind == StructureKind.LIST) {
            encodeSerializableList(descriptor, index, serializer, value)
        } else {
            encodeSerializableValue(serializer, value)
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T> encodeSerializableList(
        descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T
    ) {
        val size = descriptor.findSizeOnElement(index)
        val actualSerializer = if (size != null) {
            val maxSize = size.length

            @Suppress("UNCHECKED_CAST") val abstractCollectionSerializer =
                serializer as AbstractCollectionSerializer<T, *, *>

            val actualSize = abstractCollectionSerializer.collectionSize(value)

            if (actualSize > maxSize) {
                throw IllegalArgumentException(
                    "Collection size $actualSize is bigger than $maxSize declared on ${
                        descriptor.getElementName(
                            index
                        )
                    } in $descriptor"
                )
            }

            val constSizeSerializer = ConstByteSizeCollectionSerializationStrategy(
                serializer as SerializationStrategy<T>,
                maxSize,
                actualSize
            )

            constSizeSerializer
        } else serializer

        if (actualSerializer !is ConstByteSizeSerializationStrategy<T>) {
            throw IllegalArgumentException("An instance of ${ConstByteSizeSerializationStrategy::class} is supported got $actualSerializer")
        }

        encodeSerializableValue(actualSerializer, value)
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T> AbstractCollectionSerializer<T, *, *>.collectionSize(
        value: T
    ): Int {
        return javaClass.getMethod("collectionSize", Any::class.java).invoke(this, value) as Int
    }
}


internal fun Encoder.requireHasOutputEncoder(): HasOutput {
    return this as? HasOutput
        ?: throw IllegalArgumentException("Only ${HasOutput::class} is supported got $this")
}

internal fun Decoder.requireBinaryDecoderBase(): BinaryDecoder {
    return this as? BinaryDecoder
        ?: throw IllegalArgumentException("Only ${BinaryDecoder::class} is supported got $this")
}
