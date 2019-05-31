package jp.nephy.tweetstorm

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConcurrentMutableList<T: Any?>: AbstractMutableList<T>() {
    private val underlyingList = mutableListOf<T>()
    private val mutex = Mutex()

    override val size: Int
        get() = runBlocking {
            mutex.withLock {
                underlyingList.size
            }
        }

    override fun get(index: Int): T {
        return runBlocking {
            mutex.withLock {
                underlyingList[index]
            }
        }
    }

    override fun add(index: Int, element: T) {
        return runBlocking {
            mutex.withLock {
                underlyingList.add(index, element)
            }
        }
    }

    override fun set(index: Int, element: T): T {
        return runBlocking {
            mutex.withLock {
                underlyingList.set(index, element)
            }
        }
    }

    override fun removeAt(index: Int): T {
        return runBlocking {
            mutex.withLock {
                underlyingList.removeAt(index)
            }
        }
    }
}