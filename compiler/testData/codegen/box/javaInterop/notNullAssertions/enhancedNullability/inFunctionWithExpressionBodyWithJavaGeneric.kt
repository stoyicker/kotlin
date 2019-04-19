// !LANGUAGE: +StrictJavaNullabilityAssertions
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// SKIP_JDK6
// See KT-8135
// We could generate runtime assertion on call site for 'generic<NOT_NULL_TYPE>()' below.

// FILE: box.kt
fun box(): String {
    try {
        J().test()
        return "Fail: KotlinNullPointerException should have been thrown"
    }
    catch (e: KotlinNullPointerException) {
        return "OK"
    }
}

// FILE: test.kt
fun withAssertion(j: J) = generic<String?>(j)

fun <T> generic(j: J) = j.nullT<T>()

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    public <T> @NotNull T nullT() {
        return null;
    }

    public void test() {
        TestKt.withAssertion(this);
    }
}