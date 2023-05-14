package dev.bright.vb6serializer

import java.io.DataOutput
import java.io.DataOutputStream
import java.io.OutputStream

internal class Output private constructor(
    private val stream: ByteCountingOutputStream, private val dataOutput: DataOutputStream = DataOutputStream(stream)
) : DataOutput by dataOutput {
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
