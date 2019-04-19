fun <T : Number?> foo(): T = null as T

val x: Int = foo()

fun test() = x

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe