// WITH_RUNTIME

interface A<T> {
    fun foo(): T
}

inline fun <T> foo(): A<T> {
    return object: A<T> {
        override fun foo(): T = null as T
    }
}

fun box(): String {
    val z = try {
        foo<String>().foo()
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}
