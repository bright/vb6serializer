package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.AbstractCollectionSerializer
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal class BinaryEncoder(
    override val output: Output,
    override val serializersModule: SerializersModule,
    private val configuration: VB6BinaryConfiguration
) :
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
        if (value.isNotEmpty()) {
            val bytes = value.toByteArray(defaultSerializingCharset)

            output.write(bytes)

            val paddingChar = configuration.stringPaddingCharacterByte.toInt()
            repeat(maxLength - bytes.size) {
                output.writeByte(paddingChar)
            }
        } else {
            val paddingChar = configuration.emptyStringsPaddingCharacterByte.toInt()
            repeat(maxLength) {
                output.writeByte(paddingChar)
            }
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

            if (actualSize == 0) {
                fillZeroBytesSerializer(
                    descriptor,
                    index,
                    serializer,
                    maxSize,
                    descriptor.getElementDescriptor(index).getElementDescriptor(0)
                )
            } else {
                val constSizeSerializer = ConstByteSizeCollectionSerializationStrategy(
                    serializer as SerializationStrategy<T>,
                    maxSize,
                    actualSize
                )

                constSizeSerializer
            }
        } else serializer

        if (actualSerializer !is ConstByteSizeSerializationStrategy<T>) {
            throw IllegalArgumentException("An instance of ${ConstByteSizeSerializationStrategy::class} is supported got $actualSerializer")
        }

        encodeSerializableValue(actualSerializer, value)
    }

    private fun <T> fillZeroBytesSerializer(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: AbstractCollectionSerializer<T, *, *>,
        maxSize: Int,
        itemDescriptor: SerialDescriptor
    ): FillZeroBytesSerializer<T> {
        val itemByteSize = when (val serialKind = itemDescriptor.kind) {
            is PrimitiveKind -> when (serialKind) {
                PrimitiveKind.BOOLEAN -> Byte.SIZE_BYTES
                PrimitiveKind.BYTE -> Byte.SIZE_BYTES
                PrimitiveKind.CHAR -> Char.SIZE_BYTES
                PrimitiveKind.DOUBLE -> Double.SIZE_BYTES
                PrimitiveKind.FLOAT -> Float.SIZE_BYTES
                PrimitiveKind.INT -> Int.SIZE_BYTES
                PrimitiveKind.LONG -> Long.SIZE_BYTES
                PrimitiveKind.SHORT -> Short.SIZE_BYTES
                PrimitiveKind.STRING -> {
                    (serializer.elementSerializer() as? ConstByteSizeSerializationStrategy)?.totalByteSize
                }
            }

            else -> (serializer.elementSerializer() as? ConstByteSizeSerializationStrategy)?.totalByteSize
        }

        return if (itemByteSize != null) {
            val totalCollectionSize = itemByteSize * maxSize
            FillZeroBytesSerializer(serializer.descriptor, totalCollectionSize)
        } else {
            throw IllegalArgumentException(
                "Empty collection provided ${
                    descriptor.getElementName(
                        index
                    )
                } in $descriptor. Cannot deduce element size without reflection! Provide at least one element or a dedicated serializer e.g. ConstByteSizeCollectionKSerializer"
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T> AbstractCollectionSerializer<T, *, *>.collectionSize(
        value: T
    ): Int {
        return javaClass.getMethod("collectionSize", Any::class.java).invoke(this, value) as Int
    }

    @OptIn(InternalSerializationApi::class)
    private fun <T> AbstractCollectionSerializer<T, *, *>.elementSerializer(
    ): SerializationStrategy<*> {
        return CollectionLikeSerializer.elementSerializer(this)
    }
}

internal class FillZeroBytesSerializer<T>(override val descriptor: SerialDescriptor, override val totalByteSize: Int) :
    ConstByteSizeSerializationStrategy<T> {
    override fun serialize(encoder: Encoder, value: T) {
        repeat(totalByteSize) { encoder.encodeByte(0) }
    }
}

@OptIn(InternalSerializationApi::class)
object CollectionLikeSerializer {
    private val elementSerializer =
        javaClass.classLoader.loadClass("kotlinx.serialization.internal.CollectionLikeSerializer")
            .getDeclaredField("elementSerializer").apply { isAccessible = true }

    fun <T> elementSerializer(serializer: AbstractCollectionSerializer<T, *, *>): SerializationStrategy<*> =
        elementSerializer.get(serializer) as SerializationStrategy<*>
}


internal fun Encoder.requireHasOutputEncoder(): HasOutput {
    return this as? HasOutput
        ?: throw IllegalArgumentException("Only ${HasOutput::class} is supported got $this")
}

internal fun Decoder.requireBinaryDecoderBase(): BinaryDecoder {
    return this as? BinaryDecoder
        ?: throw IllegalArgumentException("Only ${BinaryDecoder::class} is supported got $this")
}
