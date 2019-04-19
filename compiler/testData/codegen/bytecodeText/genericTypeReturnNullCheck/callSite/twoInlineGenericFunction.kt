inline fun <T> foo(): T = null as T
inline fun <T> bar(): T = foo<T>()

fun test() = bar<String>()

// 0 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe