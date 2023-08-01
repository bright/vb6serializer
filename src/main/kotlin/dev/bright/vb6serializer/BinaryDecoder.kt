package dev.bright.vb6serializer

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayOutputStream
import java.io.EOFException

@OptIn(ExperimentalSerializationApi::class)
internal open class BinaryDecoder(
    override val input: Input,
    internal val configuration: VB6BinaryConfiguration,
    override val serializersModule: SerializersModule
) :
    Decoder, CompositeDecoder, HasInput {
    private var currentElementIndex = 0
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
        if (input.isComplete || (descriptor.kind == StructureKind.CLASS && currentElementIndex == descriptor.elementsCount)) {
            return CompositeDecoder.DECODE_DONE
        }
        return currentElementIndex.also { currentElementIndex += 1 }
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

        try {
            var byte: Int
            do {

                byte = input.readUnsignedByte()
                byteStream.write(byte)

            } while (byte != 0)
        } catch (_: EOFException) {
        }

        val contents = byteStream.toByteArray()

        return String(
            contents, 0, contents.size, defaultSerializingCharset
        )
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return BinaryDecoder(input, configuration, serializersModule)
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        val length = descriptor.findSizeOnElement(index)?.length
        return length?.let { decodeStringWithFixedByteSize(it) } ?: decodeStringWithVariableLength()
    }

    internal fun decodeStringWithFixedByteSize(byteLength: Int): String {
        val contents = ByteArray(byteLength)
        input.readFully(contents)
        val nonPaddedLength = contents.indexOf(0)
        val actualLength = if (nonPaddedLength == -1) {
            contents.size
        } else {
            nonPaddedLength
        }
        return String(
            contents, 0, actualLength, defaultSerializingCharset
        )
    }

    private fun decodeStringWithVariableLength(): String {
        val stringSize = input.readShort().toInt()
        val contents = ByteArray(stringSize)
        input.readFully(contents)
        return String(
            contents, 0, stringSize, defaultSerializingCharset
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
        return if (deserializer is ConstByteSizeDeserializationStrategy<T>) {
            decodeSerializableValue(deserializer)
        } else {
            val collectionSize = descriptor.requireSizeOnElement(index)

            val constSizeCollectionDecoder =
                ConstSizeCollectionDecoder(collectionSize.length, input, configuration, serializersModule)
            deserializer.deserialize(
                HasConstStructureDecoder(
                    input, configuration, constSizeCollectionDecoder
                )
            )
        }
    }
}
