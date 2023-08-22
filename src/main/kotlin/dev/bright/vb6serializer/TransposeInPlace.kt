package dev.bright.vb6serializer

internal fun transposeInPlace(serialized: ByteArray, rows: Int, cols: Int, elementByteSize: Int) {
    fun swapElement(i: Int, j: Int) {
        val iByteSizeIndex = i * elementByteSize
        val jByteSizeIndex = j * elementByteSize

        for (x in 0 until elementByteSize) {
            val temp = serialized[iByteSizeIndex + x]
            serialized[iByteSizeIndex + x] = serialized[jByteSizeIndex + x]
            serialized[jByteSizeIndex + x] = temp
        }
    }
    // https://stackoverflow.com/questions/9227747/in-place-transposition-of-a-matrix
    val lastIndex = rows * cols - 1
    val visited = BooleanArray(rows * cols)

    for (ix in 1 until lastIndex) {// 0 and lastIndex shouldn't move
        if (visited[ix]) {
            continue
        }

        var cycleStartIndex = ix
        do {
            cycleStartIndex = if (cycleStartIndex == lastIndex) lastIndex else (rows * cycleStartIndex) % lastIndex
            // Swap arr[a] and arr[cycleIndex]
            swapElement(cycleStartIndex, ix)
            visited[cycleStartIndex] = true
        } while (cycleStartIndex != ix)
    }
}
