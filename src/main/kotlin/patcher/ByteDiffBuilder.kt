package patcher

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.math.max

class ByteDiffBuilder(private val oldFile: String, private val newFile: String) {
    fun build(): ByteDifferenceInfo = FileInputStream(oldFile).use { ofs ->
        FileInputStream(newFile).use { nfs ->
            buildDiffList(ofs, nfs).toTypedArray().let { ByteDifferenceInfo(it, Path(newFile).fileSize()) }
        }
    }

    fun write(path: String) = FileOutputStream(path).use { fs ->
        ObjectOutputStream(fs).use { os ->
            os.writeObject(build())
            os.flush()
        }
    }

    private fun buildDiffList(oldFileStream: InputStream, newFileStream: InputStream): List<ByteDifference> {
        val oldFileSize = Path(oldFile).fileSize()
        val newFileSize = Path(newFile).fileSize()
        val maxSize = max(oldFileSize, newFileSize)

        val diffList = LinkedList<ByteDifference>()
        var currentSize = 0L
        while (currentSize < maxSize) {
            val oldByte = readByte(oldFileStream)
            val newByte = readByte(newFileStream)

            byteDiff(oldByte, newByte, currentSize)?.let { diffList += it }
            currentSize++
        }

        return diffList
    }

    private fun byteDiff(oldByte: Byte?, newByte: Byte?, index: Long): ByteDifference? = when {
        oldByte == null && newByte == null -> null
        oldByte != null && newByte == null -> ByteDifference.Removed(index)
        oldByte != null && newByte != null -> if (oldByte == newByte) null else ByteDifference.Replaced(newByte, index)
        oldByte == null && newByte != null -> ByteDifference.Added(newByte, index)
        else -> throw AssertionError()
    }
}

fun readByte(inputStream: InputStream): Byte? = inputStream.read()
    .let { if (it == -1) null else it }
    ?.toByte()