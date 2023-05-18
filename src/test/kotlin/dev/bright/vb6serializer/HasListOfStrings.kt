package dev.bright.vb6serializer

import kotlinx.serialization.Serializable

@Serializable
data class HasListOfStrings(
    @Size(6)
    val items: List<@Serializable(with = String2CharsSerializer::class) String>, // unfortunately @Size doesn't work here :( https://github.com/Kotlin/kotlinx.serialization/issues/2237
)

object String2CharsSerializer : ConstByteSizeStringSerializer(2)
