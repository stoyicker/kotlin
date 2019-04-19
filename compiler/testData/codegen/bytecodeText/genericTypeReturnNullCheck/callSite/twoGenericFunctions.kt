fun <T> foo(): T = null as T
fun <T> bar(): T = foo<T>()

fun test() = bar<String>()

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe