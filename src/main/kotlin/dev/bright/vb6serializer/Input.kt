package dev.bright.vb6serializer

import java.io.DataInput
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

internal class Input private constructor(
    private val stream: ByteCountingInputStream, private val dataInput: DataInputStream = DataInputStream(stream)
) : DataInput by dataInput {
    inline fun <T> skipBytesUpToAfterReading(expectedBytesToRead: Int, reader: () -> T): T {
        val beforeRead = stream.bytesRead
        return reader().also {
            val afterRead = stream.bytesRead
            val bytesRead = afterRead - beforeRead
            if (bytesRead < expectedBytesToRead) {
                stream.skip((expectedBytesToRead - bytesRead).toLong())
            }
        }
    }

    override fun toString(): String {
        return "Input(stream=$stream, dataInput=$dataInput)"
    }

    // TODO: that's not accurate
    val isComplete: Boolean get() = stream.available() <= 0

    companion object {
        fun create(stream: InputStream) = Input(ByteCountingInputStream(stream))
    }
}

internal class ByteCountingInputStream(private val inner: InputStream) : InputStream() {
    var bytesRead: Int = 0
        private set

    override fun read(): Int {
        return inner.read().also { bytesRead += 1 }
    }

    override fun readAllBytes(): ByteArray {
        return inner.readAllBytes().also { bytesRead += it.size }
    }

    override fun read(b: ByteArray): Int {
        return inner.read(b).also { bytesRead += it }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return inner.read(b, off, len).also { bytesRead += it }
    }

    override fun readNBytes(len: Int): ByteArray {
        return inner.readNBytes(len).also { bytesRead += it.size }
    }

    override fun readNBytes(b: ByteArray?, off: Int, len: Int): Int {
        return inner.readNBytes(b, off, len).also { bytesRead += it }
    }

    override fun skip(n: Long): Long {
        return inner.skip(n).also { bytesRead += it.toInt() }
    }

    override fun skipNBytes(n: Long) {
        inner.skipNBytes(n).also { bytesRead += n.toInt() }
    }

    override fun mark(readlimit: Int) {
        inner.mark(readlimit)
    }

    override fun transferTo(out: OutputStream?): Long {
        return inner.transferTo(out).also { bytesRead += it.toInt() }
    }

    override fun close() {
        inner.close()
    }

    override fun available(): Int {
        return inner.available()
    }

    override fun reset() {
        return inner.reset()
    }

    override fun markSupported(): Boolean {
        return inner.markSupported()
    }
}

