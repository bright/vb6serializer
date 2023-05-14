package dev.bright.vb6serializer

import java.io.OutputStream

internal class ByteCountingOutputStream(private val inner: OutputStream) : OutputStream() {
    var bytesWritten: Int = 0
        private set

    override fun write(b: Int) {
        inner.write(b)
        bytesWritten += 1
    }

    override fun write(b: ByteArray) {
        inner.write(b)
        bytesWritten += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        inner.write(b, off, len)
        bytesWritten += len
    }

    override fun close() {
        inner.close()
    }

    override fun flush() {
        inner.flush()
    }
}
