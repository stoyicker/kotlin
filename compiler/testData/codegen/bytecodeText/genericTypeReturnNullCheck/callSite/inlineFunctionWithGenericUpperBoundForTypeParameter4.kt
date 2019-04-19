class Foo<K> {
    inline fun <reified T : K> foo(): T = null as T
}

fun main() {
    val x: Int = Foo<Number>().foo()
}

// 0 IFNONNULL
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe