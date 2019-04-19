class Foo<K> {
    fun <T : K> foo(): T = null as T
}

fun main() {
    val x: Int = Foo<Number>().foo()
}

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe