package dev.bright.vb6serializer

import kotlinx.serialization.descriptors.SerialDescriptor

interface DynamicSizeResolver {
    fun sizeFor(descriptor: SerialDescriptor, index: Int): Size?

    companion object {
        val Null = object : DynamicSizeResolver {
            override fun sizeFor(descriptor: SerialDescriptor, index: Int) = null
        }
    }
}
