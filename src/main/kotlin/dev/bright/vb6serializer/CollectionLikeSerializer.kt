package dev.bright.vb6serializer

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.internal.AbstractCollectionSerializer

@OptIn(InternalSerializationApi::class)
internal object CollectionLikeSerializer {
    private val elementSerializer =
        javaClass.classLoader.loadClass("kotlinx.serialization.internal.CollectionLikeSerializer")
            .getDeclaredField("elementSerializer").apply { isAccessible = true }

    fun <T> elementSerializer(serializer: AbstractCollectionSerializer<T, *, *>): SerializationStrategy<*> =
        elementSerializer.get(serializer) as SerializationStrategy<*>
}

@OptIn(InternalSerializationApi::class)
internal fun <T> AbstractCollectionSerializer<T, *, *>.elementSerializer(
): SerializationStrategy<*> {
    return CollectionLikeSerializer.elementSerializer(this)
}
