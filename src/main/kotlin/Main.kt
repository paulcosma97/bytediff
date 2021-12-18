import java.nio.file.Files
import kotlin.io.*;
import kotlin.io.path.Path
import kotlin.math.max

const val OldBinPath = "C:\\Users\\paulc\\Projects\\bytebin\\bin\\Debug\\net6.0\\bytebin-old.dll"
const val NewBinPath = "C:\\Users\\paulc\\Projects\\bytebin\\bin\\Debug\\net6.0\\bytebin-new.dll"

fun main(args: Array<String>) {
    var oldFile = Files.readAllBytes(Path(OldBinPath)).toList().toLinkedList()
    val newFile = Files.readAllBytes(Path(NewBinPath))

    var sameBytesCount = 0;
    val byteDiff: LinkedListImpl<ByteDiff2> = LinkedListImpl(null)
    for (idx in newFile.indices) {
        val oldByte = oldFile.getOrNull(idx)
        val newByte = newFile.getOrNull(idx)

        if (newByte == oldByte) {
            sameBytesCount += 1
            continue
        }

        when {
            oldByte == null && newByte != null -> {
                byteDiff += ByteDiff2.Added(newByte, idx)
            }
            oldByte != null && newByte == null -> {
                byteDiff += ByteDiff2.Removed(idx)
            }
            oldByte != null && newByte != null -> {
                byteDiff += ByteDiff2.Replaced(newByte, idx)
            }
            else -> {
                sameBytesCount += 1
            }
        }
    }

    println(Diff2 (
        oldFileSize = oldFile.size,
        newFileSize = newFile.size,
        sameBytes = sameBytesCount,
        diffBytes = max(oldFile.size, newFile.size) - sameBytesCount,
        newBytes = byteDiff.count { it.value is ByteDiff2.Added },
        replacedBytes = byteDiff.count { it.value is ByteDiff2.Replaced },
        removedBytes = byteDiff.count { it.value is ByteDiff2.Removed },
    ))

    for (diffByte in byteDiff) {
        when (val diff = diffByte.value) {
            is ByteDiff2.Added -> {
                oldFile += diff.byte
            }
            is ByteDiff2.Replaced -> {
                oldFile[diff.index] = diff.byte
            }
            is ByteDiff2.Removed -> {
                oldFile -= diff.index
            }
        }
    }

    Files.write(Path("./out.dll"), oldFile.toList().toByteArray())
}


data class Diff2(val oldFileSize: Int, val newFileSize: Int, val sameBytes: Int, val diffBytes: Int, val newBytes: Int, val replacedBytes: Int, val removedBytes: Int)
sealed class ByteDiff2(val index: Int) {
    class Replaced(val byte: Byte, index: Int): ByteDiff2(index)
    class Removed(index: Int): ByteDiff2(index)
    class Added(val byte: Byte, index: Int): ByteDiff2(index)
}

class LinkedListImpl<T>(var maybeRoot: Node<T>?) : Iterable<Node<T>> {
    override fun iterator(): Iterator<Node<T>> {
        return maybeRoot?.iterator() ?: object : Iterator<Node<T>> {
            override fun hasNext(): Boolean = false
            override fun next(): Node<T> = throw Exception()
        }
    }

    val size get() = maybeRoot?.size ?: 0

    fun getOrNull(index: Int): T? = maybeRoot?.getOrNull(index)?.value
    operator fun get(index: Int): T = maybeRoot!![index].value
    operator fun set(index: Int, value: T) {
        maybeRoot!![index].replaceWith(value)
    }

    operator fun plusAssign(value: T) {
        if (maybeRoot == null) {
            maybeRoot = Node(null, null, value)
        }

        maybeRoot!!.last.addAfter(value)
    }

    operator fun minusAssign(index: Int) {
        if (maybeRoot == null) {
            throw Exception("linked list is empty")
        }

        maybeRoot = maybeRoot!![index].remove()
    }

    fun toList(): List<T> {
        if (maybeRoot == null) {
            return emptyList()
        }

        val out = mutableListOf<T>()
        for (node in maybeRoot!!) {
            out += node.value
        }
        return out
    }
}

fun <T> List<T>.toLinkedList(): LinkedListImpl<T> {
    if (size == 0) {
        return LinkedListImpl(null)
    }

    var lastNode = Node(null, null, get(0))
    for(idx in indices) {
        if (idx == 0) {
            continue
        }

        lastNode = lastNode.addAfter(get(idx))
    }

    return LinkedListImpl(lastNode.root)
}

open class Node<T>(var previous: Node<T>?, var next: Node<T>?, var value: T) : Iterable<Node<T>> {
    override fun iterator(): Iterator<Node<T>> {
        var node = root;
        return object : Iterator<Node<T>> {
            override fun hasNext(): Boolean {
                return node.next != null
            }

            override fun next(): Node<T> {
                node = node.next!!

                return node
            }
        }
    }

    fun replaceWith(value: T) {
        this.value = value
    }

    fun addAfter(value: T): Node<T> {
        if (isLast) {
            next = Node(this, null, value)
            return next!!
        }

        val oldNext = next
        next = Node(this, oldNext, value)
        oldNext!!.previous = next
        return next!!
    }

    fun remove(): Node<T>? {
        val newRoot: Node<T>? =  when {
            isFirst && isLast -> null
            isFirst && !isLast -> {
                val newRoot = next
                next!!.previous = previous
                next = null
                return newRoot
            }
            !isFirst && isLast -> {
                val newRoot = previous
                previous!!.next = null
                previous = null
                return newRoot
            }
            !isFirst && !isLast -> {
                val newRoot = previous!!
                previous!!.next = next
                next!!.previous = previous
                next = null
                previous = null
                return newRoot
            }
            else -> throw Exception()
        }

        return newRoot?.root
    }

    val isLast get() = next == null
    val isFirst get() = previous == null

    val root: Node<T> get() {
        var root = this;
        while(root.previous != null) {
            root = root.previous!!
        }
        return root;
    }

    val last: Node<T> get() {
        var current = this
        while(current.next != null) {
            current = current.next!!
        }
        return current
    }

    val size: Int get() {
        var node = root
        var count = 0
        while (node.next != null) {
            count += 1
            node = node.next!!
        }
        return count
    }

    operator fun get(index: Int): Node<T> {
        if (index >= size) {
            throw IndexOutOfBoundsException(index)
        }

        var node = root
        var count = 0
        while (count < index) {
            if (count == index) {
                return node
            }

            count++
            node = node.next!!
        }

        throw Exception()
    }

    fun getOrNull(index: Int): Node<T>? {
        return try {
            get(index)
        } catch (e: java.lang.Exception) {
            null
        }
    }
}
