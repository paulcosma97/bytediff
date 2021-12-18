package patcher

import java.io.Serializable

sealed class ByteDifference(val index: Long): Serializable {
    class Replaced(val byte: Byte, index: Long): ByteDifference(index)
    class Removed(index: Long): ByteDifference(index)
    class Added(val byte: Byte, index: Long): ByteDifference(index)
}

class ByteDifferenceInfo(val diffArray: Array<ByteDifference>, val totalSize: Long): Serializable