val map = mapOf<String, Any?>("x" to null)
val x: String by map

fun test() = x

// 3 DUP
// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe
