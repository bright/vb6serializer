package dev.bright.vb6serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.SerialDescriptor

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
annotation class Size(val length: Int)

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.requireSizeOnElement(
    elementIndex: Int
) = findSizeOnElement(elementIndex) ?: throw IllegalArgumentException(
    "In order to serialize $this the ${getElementName(elementIndex)}: ${
        getElementDescriptor(
            elementIndex
        )
    } must be annotated with @${Size::class}"
)

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.findSizeOnElement(elementIndex: Int) =
    getElementAnnotations(elementIndex).firstNotNullOfOrNull { it as? Size }
