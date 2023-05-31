package dev.bright.vb6serializer

import java.io.DataInput
import java.io.DataOutput

internal class LittleEndianDataInputStream(private val inner: DataInput) : DataInput by inner {
    override fun readInt(): Int = inner.readInt().reverseBytes()
    override fun readLong(): Long = inner.readLong().reverseBytes()
    override fun readShort(): Short = inner.readShort().reverseBytes()
    override fun readFloat(): Float = inner.readFloat().reverseBytes()
    override fun readDouble(): Double = inner.readDouble().reverseBytes()
    override fun readUnsignedShort(): Int {
        TODO("Not yet implemented")
    }
}

internal class LittleEndianDataOutputStream(private val inner: DataOutput) : DataOutput by inner {
    override fun writeInt(v: Int) {
        inner.writeInt(v.reverseBytes())
    }

    override fun writeShort(v: Int) {
        inner.writeShort(v.toShort().reverseBytes().toInt())
    }

    override fun writeLong(v: Long) {
        inner.writeLong(v.reverseBytes())
    }

    override fun writeFloat(v: Float) {
        inner.writeFloat(v.reverseBytes())
    }

    override fun writeDouble(v: Double) {
        inner.writeDouble(v.reverseBytes())
    }
}

private fun Int.reverseBytes(): Int = Integer.reverseBytes(this)
private fun Long.reverseBytes(): Long = java.lang.Long.reverseBytes(this)
private fun Short.reverseBytes(): Short = java.lang.Short.reverseBytes(this)
private fun Float.reverseBytes(): Float = Float.fromBits(this.toBits().reverseBytes())
private fun Double.reverseBytes(): Double = Double.fromBits(this.toBits().reverseBytes())
