package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

internal class HasConstStructureDecoder(
    input: Input, configuration: VB6BinaryConfiguration, private val structureDecoder: CompositeDecoder
) : BinaryDecoder(input, configuration, structureDecoder.serializersModule) {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return structureDecoder
    }
}

internal open class ConstSizeCollectionDecoder(
    private val collectionSize: Int,
    input: Input,
    configuration: VB6BinaryConfiguration,
    serializersModule: SerializersModule,
) : BinaryDecoder(input, configuration, serializersModule) {

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = collectionSize

    @ExperimentalSerializationApi
    override fun decodeSequentially(): Boolean = true
}
