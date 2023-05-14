package dev.bright.vb6serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalSerializationApi::class)
internal open class BinaryDecoderBase(protected val input: Input, override val serializersModule: SerializersModule) :
    Decoder, CompositeDecoder {
    private var currentElementIndex = -1
    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return input.readBoolean()
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return input.readByte()
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return input.readChar()
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return input.readDouble()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (input.isComplete) {
            return CompositeDecoder.DECODE_DONE
        }
        currentElementIndex += 1
        return currentElementIndex
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return input.readFloat()
    }

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented")
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return input.readInt()
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return input.readLong()
    }

    @ExperimentalSerializationApi
    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?
    ): T? {
        TODO("Not yet implemented")
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return input.readShort()
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    override fun decodeBoolean(): Boolean {
        return input.readBoolean()
    }

    override fun decodeByte(): Byte {
        return input.readByte()
    }

    override fun decodeChar(): Char {
        return input.readChar()
    }

    override fun decodeDouble(): Double {
        return input.readDouble()
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
    }

    override fun decodeFloat(): Float {
        return input.readFloat()
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented")
    }

    override fun decodeInt(): Int {
        return input.readInt()
    }

    override fun decodeLong(): Long {
        return input.readLong()
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
    }

    override fun decodeShort(): Short {
        return input.readShort()
    }

    override fun decodeString(): String {
        val byteStream = ByteArrayOutputStream(16)
        var byte: Int
        do {
            byte = input.readUnsignedByte()
            byteStream.write(byte)
        } while (byte != 0)

        val contents = byteStream.toByteArray()
        return String(
            contents, 0, contents.size, serializingCharset
        )
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        val length = descriptor.requireSizeOnElement(index)
        return decodeStringWithLength(length.length)
    }

    protected fun decodeStringWithLength(byteLength: Int): String {
        val contents = ByteArray(byteLength)
        input.readFully(contents)
        val nonPaddedLength = contents.indexOf(0)
        val actualLength = if (nonPaddedLength == -1) {
            contents.size
        } else {
            nonPaddedLength
        }
        return String(
            contents, 0, actualLength, serializingCharset
        )
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?
    ): T {
        if (descriptor.getElementDescriptor(index).kind == StructureKind.LIST) {
            return decodeSerializableList(descriptor, index, deserializer)
        }
        return decodeSerializableValue(deserializer)
    }

    private fun <T> decodeSerializableList(
        descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>
    ): T {
        val collectionSize = descriptor.requireSizeOnElement(index)

        val listDescriptor = descriptor.getElementDescriptor(index)

        val listElementDescriptor = listDescriptor.elementDescriptors.firstOrNull()

        if (listElementDescriptor?.kind == PrimitiveKind.STRING) { // strings are the only primitives with variable length
            val explicitListElementSize = descriptor.findElementSizeOnElement(index)
            val elementByteSize = explicitListElementSize?.length ?: listDescriptor.listElementByteSize()

            return deserializer.deserialize(
                HasFixedStructureDecoder(
                    input, FixedSizeStringCollectionOfSizeDecoder(
                        collectionSize.length, input, serializersModule, elementByteSize
                    )
                )
            )
        }

        return deserializer.deserialize(
            HasFixedStructureDecoder(
                input, FixedSizeCollectionSizeDecoder(collectionSize.length, input, serializersModule)
            )
        )
    }
}
