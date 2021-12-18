package patcher

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.nio.file.Paths

class ByteDiffResolver(val oldFilePath: String, val diffInfo: ByteDifferenceInfo) {
    fun applyDiff(path: String) {
        FileInputStream(oldFilePath).use { fs ->
            var file = File(path)
            var restorableFileName: String?

            try {
                if (!file.canWrite() || !file.canRead()) {
                    throw IOException("Not enough permissions to read and write to file '$path'.")
                }

                if (file.exists()) {
                    restorableFileName = file.renameToRestorable()
                    file = File(path)
                    assert(file.createNewFile())
                }

                var byte: Byte?

                do {
                    byte = readByte(fs)
                } while (byte != null)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    fun File.renameToRestorable(): String {
        var extensi
    }
}