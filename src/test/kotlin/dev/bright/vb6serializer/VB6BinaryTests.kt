@file:OptIn(ExperimentalSerializationApi::class)

package dev.bright.vb6serializer

import io.kotest.matchers.shouldBe
import kotlinx.serialization.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class VB6BinaryTests {

    @Test
    @Disabled("Without max length encoding we aren't able to serialize properly")
    fun `can serialize a string`() {
        // given
        val input = "VB6 is fun! "
        // when
        val serialized = VB6Binary.encodeToByteArray(input)
        val deserialized = VB6Binary.decodeFromByteArray<String>(serialized)

        // then
        deserialized.shouldBe(input)
    }

    @Test
    fun `can serialize object with name`() {
        // given
        val input = HasName("Ala")
        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }

    @Test
    fun `can serialize object with name and age`() {
        // given
        val input = HasNameAndAge("Ala", 12)

        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }

    @Test
    fun `can serialize object with name and nested object`() {
        // given
        val input = HasNameAndNestedObject("Ala", HasNameAndAge("Ala", 12))

        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }


    @Test
    fun `can serialize object with int array`() {
        // given
        val input = HasIntArray(intArrayOf(1, 2, 3, 4), 42)

        // when
        val output = serde(input)

        // then
        output.shouldBe(HasIntArray(intArrayOf(1, 2, 3, 4, 0 /* padded because of length */), 42))
    }

    @Test
    fun `can serialize object with short array`() {
        // given
        val input = HasShortArray(shortArrayOf(1, 2, 3, 4), "Bright")

        // when
        val output = serde(input)

        // then
        output.shouldBe(HasShortArray(shortArrayOf(1, 2, 3, 4, 0 /* padded because of length */), "Bright"))
    }

    @Test
    fun `can serialize object with list of bytes array`() {
        // given
        val input = HasListOfBytes(listOf(1, 3, 3, 4))

        // when
        val output = serde(input)

        // then
        output.shouldBe(HasListOfBytes(listOf(1, 3, 3, 4, 0, 0 /* padded */)))
    }

    @Test
    fun `can serialize object with list of strings`() {
        // given
        val input = HasListOfStrings(listOf("A", "B", "C"))

        // when
        val output = serde(input)

        // then
        output.shouldBe(HasListOfStrings(listOf("A", "B", "C", "", "", "")))
        // NOTE: should this be the case?
        // We could technically transform [0,0] --deserialize--> (String) null
        // However, that would mean that the following is true
        // @Size(2) String "" --serialise--> [0,0] --deserialize--> (String) null
    }

    @Serializable
    data class HasName(@Size(10) val name: String)

    @Serializable
    data class HasNameAndAge(@Size(10) val name: String, val age: Int)

    @Serializable
    data class HasNameAndNestedObject(@Size(10) val name: String, val info: HasNameAndAge)

    @Serializable
    data class HasIntArray(
        @Size(5)
        val array: IntArray,
        val other: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HasIntArray

            if (!array.contentEquals(other.array)) return false
            return this.other == other.other
        }

        override fun hashCode(): Int {
            var result = array.contentHashCode()
            result = 31 * result + other
            return result
        }
    }

    @Serializable
    data class HasShortArray(
        @Size(5)
        val array: ShortArray,
        @Size(6)
        val other: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HasShortArray

            if (!array.contentEquals(other.array)) return false
            return this.other == other.other
        }

        override fun hashCode(): Int {
            var result = array.contentHashCode()
            result = 31 * result + other.hashCode()
            return result
        }
    }

    @Serializable
    data class HasListOfBytes(
        @Size(6)
        val items: List<Byte>,
    )


}

inline fun <reified T : Any> serde(input: T): T {
    val serialized = VB6Binary.encodeToByteArray(input)
    return VB6Binary.decodeFromByteArray<T>(serialized)
}

