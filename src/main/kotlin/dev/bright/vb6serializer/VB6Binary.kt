package dev.bright.vb6serializer

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
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
        val reader = BinaryDecoder(input, serializersModule)
        return reader.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val dumper = BinaryEncoder(Output.create(outputStream), serializersModule)
        dumper.encodeSerializableValue(serializer, value)
        return outputStream.toByteArray()
    }
}

internal val serializingCharset = Charset.forName("ISO-8859-8")
