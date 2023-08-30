package dev.bright.vb6serializer

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

data class VB6BinaryConfiguration(
    val encoding: Charset = defaultSerializingCharset,
    val stringPaddingCharacter: Char = ' ',
    // this is not line with what VB6 does natively
    // in VB when we assign myVar.label = ""
    // it is going to be padded with ' ' up to max length
    // However, our Kotlin code isn't prepared for `String?`
    // when reading `00000` as `String` of length 5 we end up having `""`
    // If we'll support `String?` we could very well treat zeros as `null`
    val emptyStringsPaddingCharacter: Char = 0.toChar(),
    val sizeResolver: DynamicSizeResolver = DynamicSizeResolver.Null
) {

    val stringPaddingCharacterByte get() = stringPaddingCharacter.code.toByte()
    val emptyStringsPaddingCharacterByte get() = emptyStringsPaddingCharacter.code.toByte()
}

open class VB6Binary(
    override val serializersModule: SerializersModule,
    private val configuration: VB6BinaryConfiguration
) : BinaryFormat {
    companion object Default : VB6Binary(EmptySerializersModule(), VB6BinaryConfiguration()) {
        fun <T> byteSizeOf(serializer: KSerializer<T>, instance: T) = encodeToByteArray(serializer, instance).size
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val inputStream = ByteArrayInputStream(bytes)
        val input = Input.create(inputStream)
        val reader = BinaryDecoder(input, configuration, serializersModule)
        return reader.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return serializedBytesOf(serializersModule, configuration, serializer, value)
    }
}

internal fun <T> serializedBytesOf(
    serializersModule: SerializersModule,
    configuration: VB6BinaryConfiguration,
    serializer: SerializationStrategy<T>,
    value: T
): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val dumper = BinaryEncoder(Output.create(outputStream), serializersModule, configuration)
    dumper.encodeSerializableValue(serializer, value)
    return outputStream.toByteArray()
}

internal val defaultSerializingCharset = Charset.forName("ISO-8859-8")
