package dev.bright.vb6serializer

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

@ExperimentalSerializationApi
sealed class VB6Binary(override val serializersModule: SerializersModule) : BinaryFormat {
    companion object Default : VB6Binary(EmptySerializersModule())

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val inputStream = ByteArrayInputStream(bytes)
        val input = Input.create(inputStream)
        val reader = BinaryReader(this, input)
        return reader.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val dumper = BinaryWriter(this, Output.create(outputStream))
        dumper.encodeSerializableValue(serializer, value)
        return outputStream.toByteArray()
    }
}

internal val serializingCharset = Charset.forName("ISO-8859-8")

internal class HasFixedStructureDecoder(
    input: Input, private val structureDecoder: CompositeDecoder
) : BinaryDecoderBase(input, structureDecoder.serializersModule) {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return structureDecoder
    }
}

internal class FixedSizeStringCollectionOfSizeDecoder(
    collectionSize: Int,
    input: Input,
    serializersModule: SerializersModule,
    private val elementByteSize: Int? = null
) : FixedSizeCollectionSizeDecoder(collectionSize, input, serializersModule) {

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?
    ): T {
        val elementByteSize =
            elementByteSize ?: return super.decodeSerializableElement(descriptor, index, deserializer, previousValue)

        @Suppress("UNCHECKED_CAST")
        return input.skipBytesUpToAfterReading(elementByteSize) {
            decodeStringWithLength(elementByteSize)
        } as T
    }
}

internal open class FixedSizeCollectionSizeDecoder(
    private val collectionSize: Int,
    input: Input,
    serializersModule: SerializersModule,
) : BinaryDecoderBase(input, serializersModule) {
    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = collectionSize

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true
}

@OptIn(ExperimentalSerializationApi::class)
internal class BinaryReader(
    private val vB6Binary: VB6Binary,
    input: Input,
    serializersModule: SerializersModule = vB6Binary.serializersModule,
) : BinaryDecoderBase(input, serializersModule) {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return BinaryReader(vB6Binary, input)
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.requireSizeOnElement(
    elementIndex: Int
) = getElementAnnotations(elementIndex).firstNotNullOfOrNull { it as? Size } ?: throw IllegalArgumentException(
    "In order to serialize $this the ${getElementName(elementIndex)}: ${
        getElementDescriptor(
            elementIndex
        )
    } must be annotated with @${Size::class}"
)


@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.findElementSizeOnElement(
    elementIndex: Int
) = getElementAnnotations(elementIndex).firstNotNullOfOrNull { it as? ElementSize }
