package dev.bright.vb6serializer

import java.io.DataOutput
import java.io.DataOutputStream
import java.io.OutputStream

internal class Output private constructor(
    private val stream: ByteCountingOutputStream, private val dataOutput: DataOutput = LittleEndianDataOutputStream(DataOutputStream(stream))
) : DataOutput by dataOutput {
    inline fun addPaddingWithRatio(actualSizeNominator: Int, expectedSizeDenominator: Int, bytesWriter: () -> Unit) {
        val beforeCount = stream.bytesWritten
        bytesWriter()
        val afterCount = stream.bytesWritten
        val writtenBytes = afterCount - beforeCount
        val bytesPerUnit = writtenBytes / actualSizeNominator
        val padding = ByteArray(bytesPerUnit)
        (actualSizeNominator until expectedSizeDenominator).forEach { _ ->
            stream.write(padding)
        }
    }

    inline fun addPaddingAfterBytesWritten(expectedWrittenDelta: Int, bytesWriter: () -> Unit) {
        val beforeCount = stream.bytesWritten
        bytesWriter()
        val afterCount = stream.bytesWritten
        val writtenBytes = afterCount - beforeCount
        (writtenBytes until expectedWrittenDelta).forEach { _ ->
            stream.write(0)
        }
    }

    override fun toString(): String {
        return "Output(stream=$stream, dataOutput=$dataOutput)"
    }

    companion object {
        fun create(output: OutputStream) = Output(ByteCountingOutputStream(output))
    }
}


internal interface HasOutput {
    val output: Output
}

