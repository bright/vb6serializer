package dev.bright.vb6serializer

import kotlinx.serialization.Serializable

@Serializable
data class HasListOfStrings(
    @Size(6)
    @ElementSize(2)
    val items: List</* @Size(2) */ String>, // unfortunately @Size doesn't work here :( https://github.com/Kotlin/kotlinx.serialization/issues/2237
)
