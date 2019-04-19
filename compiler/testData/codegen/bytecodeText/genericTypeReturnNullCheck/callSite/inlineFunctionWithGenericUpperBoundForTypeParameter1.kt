class Foo<K> {
    inline fun <T : K> foo(): T = null as T
}

fun main() {
    Foo<Number>().foo<Int>()
}

// 0 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe