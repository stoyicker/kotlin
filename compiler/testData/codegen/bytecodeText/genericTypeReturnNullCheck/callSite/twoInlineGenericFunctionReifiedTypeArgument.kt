inline fun <reified T> foo(): T = null as T
inline fun <reified T> bar(): T = foo<T>()

fun test() = bar<String>()

// 0 IFNONNULL
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe