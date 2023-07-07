package dev.bright.vb6serializer

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test

class VB6BinaryTests {

    @Test
//    @Disabled("Without max length encoded in output we aren't able to ser/de safely, are we?")
    fun `can serialize a string`() {
        // given
        val input = "VB6 is fun! "
        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }

    @Test
    fun `can serialize object with name`() {
        // given
        val input = HasName("Ala")
        // when
        val output = serde(input)

        // then
        output.shouldBe(HasName("Ala".padEnd(10)))
    }

    @Test
    fun `can serialize object with variable size name`() {
        // given
        val input = HasVariableSizeName("Ala")
        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `should serialize variable size string with a short in front that indicates its length`() {
        // given
        val input = HasVariableSizeName("abc")
        // when
        val output = VB6Binary.encodeToByteArray(input)

        // then
        output shouldBe byteArrayOf(0x03, 0x00, 0x61, 0x62, 0x63)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `should serialize variable size string with only a short 00 if it's empty`() {
        // given
        val input = HasVariableSizeName("")
        // when
        val output = VB6Binary.encodeToByteArray(input)

        // then
        output.shouldBe(byteArrayOf(0x00, 0x00))
    }

    @Test
    fun `can serialize object with empty name`() {
        // given
        val input = HasVariableSizeName("")
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
        output.shouldBe(HasNameAndAge("Ala".padEnd(10), 12))
    }

    @Test
    fun `can serialize object with variable size name and age`() {
        // given
        val input = HasVariableSizeNameAndAge("Ala", 12)

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
        output.shouldBe(HasNameAndNestedObject("Ala".padEnd(10), HasNameAndAge("Ala".padEnd(10), 12)))
    }

    @Test
    fun `can serialize object with variable size name and nested object`() {
        // given
        val input = HasVariableSizeNameAndNestedObject("Ala", HasVariableSizeNameAndAge("Ala", 12))

        // when
        val output = serde(input)

        // then
        output.shouldBe(input)
    }

    @Test
    fun `can serialize object with empty name and nested object`() {
        // given
        val input = HasVariableSizeNameAndNestedObject("", HasVariableSizeNameAndAge("Ala", 12))

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

    @Test
    fun `can serialize empty object with list of strings`() {
        // given
        val input = HasListOfStrings(emptyList())

        // when
        val output = serde(input)

        // then
        output.shouldBe(HasListOfStrings(listOf("", "", "", "", "", "")))
        // NOTE: should this be the case?
        // We could technically transform [0,0] --deserialize--> (String) null
        // However, that would mean that the following is true
        // @Size(2) String "" --serialise--> [0,0] --deserialize--> (String) null
    }

    @Test
    fun `can serialize int array inside array with list of strings`() {
        // given
        val input = HasListOfIntArrays(
            listOf(
                IntArray(3) { it + 1 }
            )
        )

        // when
        val output = serde(input)

        // then
        output.items.shouldHaveSize(HasListOfIntArrays.HasListOfIntArraysItemSize)
        output.items[0].shouldBe(input.items[0])
        output.items[1].shouldBe(IntArray(3) { 0 })
    }

    @Test
    fun `can serialize has list of has item code`() {
        // given
        val input = HasListOfHasItemCode(
            listOf(
                HasItemCode("P1"),
                HasItemCode("P23")
            )
        )

        // when
        val output = serde(input)

        // then
        output.items.shouldHaveSize(HasListOfHasItemCode.ItemsSize)
        output.items[0].itemCode.shouldBe("P1".padEnd(HasItemCode.ItemCodeSize))
        output.items[1].itemCode.shouldBe("P23".padEnd(HasItemCode.ItemCodeSize))
        output.items[2].itemCode.shouldBe("")
    }


}

@Serializable
data class HasItemCode(@Size(ItemCodeSize) val itemCode: String) {
    companion object {
        const val ItemCodeSize = 4
    }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> serde(input: T): T {
    val serialized = VB6Binary.encodeToByteArray(input)
    println("Serialized \n\t$input\nas\n\t${serialized.contentToString()}")
    return VB6Binary.decodeFromByteArray<T>(serialized)
}

@Serializable
data class HasName(@Size(10) val name: String)

@Serializable
data class HasNameAndAge(@Size(10) val name: String, val age: Int)

@Serializable
data class HasNameAndNestedObject(@Size(10) val name: String, val info: HasNameAndAge)

@Serializable
data class HasVariableSizeName(val name: String)

@Serializable
data class HasVariableSizeNameAndAge(val name: String, val age: Int)

@Serializable
data class HasVariableSizeNameAndNestedObject(val name: String, val info: HasVariableSizeNameAndAge)

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


@Serializable
data class HasListOfIntArrays(
    @Size(HasListOfIntArraysItemSize)
    val items: List<@Serializable(with = IntArrayWith3ElementsSerializer::class) IntArray>,
) {
    companion object {
        const val HasListOfIntArraysItemSize = 2
    }

    override fun toString(): String {
        return "HasListOfIntArrays(items=${items.map { it.contentToString() }})"
    }


}


@Serializable
data class HasListOfHasItemCode(
    @Size(ItemsSize)
    val items: List<HasItemCode>,
) {
    companion object {
        const val ItemsSize = 3
    }
}

object IntArrayWith3ElementsSerializer : ConstByteSizeCollectionKSerializer<IntArray>(
    IntArraySerializer(),
    elementByteSize = Int.SIZE_BYTES,
    collectionMaxSize = 3
)

