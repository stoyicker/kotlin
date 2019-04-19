class Foo<T> {
    inner class Bar {
        fun foo(): T = null as T
    }
}

fun test() {
    val x = Foo<String>()
    val y = x.Bar()
    y.foo()
}

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe