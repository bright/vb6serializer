package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.AbstractCollectionSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.full.memberProperties

internal open class BinaryEncoderBase(protected val output: Output, override val serializersModule: SerializersModule) :
    Encoder, CompositeEncoder {
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

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeSerializableValue(serializer, value)
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
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal class BinaryWriter(
    vB6Binary: VB6Binary,
    output: Output,
    serializersModule: SerializersModule = vB6Binary.serializersModule
) : BinaryEncoderBase(output, serializersModule) {

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T
    ) {
        if (descriptor.getElementDescriptor(index).kind == StructureKind.LIST) {
            encodeSerializableList(descriptor, index, serializer, value)
        } else {
            encodeSerializableValue(serializer, value)
        }
    }

    private fun <T> encodeSerializableList(
        descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T
    ) {
        val maxSize = descriptor.requireSizeOnElement(index).length

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

        val elementDescriptor = descriptor.getElementDescriptor(index)

        val explicitListElementSize = descriptor.findElementSizeOnElement(index)

        val elementByteSize = explicitListElementSize?.length ?: elementDescriptor.listElementByteSize()

        val collectionWithElementSizeEncoder = CollectionWithElementSizeEncoder(
            elementByteSize, output, serializersModule
        )

        serializer.serialize(HasFixedStructureEncoder(output, collectionWithElementSizeEncoder), value)
        // padding
        ByteArraySerializer().serialize(this, ByteArray((maxSize - actualSize) * elementByteSize))
    }

    private fun <T> AbstractCollectionSerializer<T, *, *>.collectionSize(
        value: T
    ): Int {
        return javaClass.getMethod("collectionSize", Any::class.java).invoke(this, value) as Int
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal data class DelegatingSerialDescriptor(
    val inner: SerialDescriptor
) : SerialDescriptor by inner {
    @ExperimentalSerializationApi
    override fun getElementAnnotations(index: Int): List<Annotation> {
        return inner.getElementAnnotations(index)
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal data class DelegatingSerializer<T>(
    val inner: SerializationStrategy<T>,
    override val descriptor: SerialDescriptor
) : SerializationStrategy<T> {
    override fun serialize(encoder: Encoder, value: T) {
        inner.serialize(encoder, value)
    }
}

internal class HasFixedStructureEncoder(
    output: Output,
    private val structureEncoder: CompositeEncoder
) : BinaryEncoderBase(output, structureEncoder.serializersModule) {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return structureEncoder
    }
}

internal class CollectionWithElementSizeEncoder(
    private val elementByteSize: Int,
    output: Output,
    serializersModule: SerializersModule
) : BinaryEncoderBase(output, serializersModule) {
    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        output.addPaddingAfterBytesWritten(elementByteSize) {
            super.encodeSerializableElement(descriptor, index, serializer, value)
        }
    }
}

fun Any.propertyByName(elementName: String) = javaClass
    .kotlin
    .memberProperties
    .find { it.name == elementName }

