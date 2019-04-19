fun <T> foo(): T = null as T

val x = foo<String>()

fun test() = x

// 1 IFNONNULL
// 1 INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe

/*
@SimpleFunctionKt.class
// class version 50.0 (50)
// access flags 0x31
public final class SimpleFunctionKt {


  // access flags 0x19
  // signature <T:Ljava/lang/Object;>()TT;
  // declaration: T foo<T>()
  public final static foo()Ljava/lang/Object;
   L0
    LINENUMBER 1 L0
    ACONST_NULL
    ARETURN
   L1
    MAXSTACK = 1
    MAXLOCALS = 0

  // access flags 0x1A
  private final static Ljava/lang/String; x
  @Lorg/jetbrains/annotations/NotNull;() // invisible

  // access flags 0x19
  public final static getX()Ljava/lang/String;
  @Lorg/jetbrains/annotations/NotNull;() // invisible
   L0
    LINENUMBER 3 L0
    GETSTATIC SimpleFunctionKt.x : Ljava/lang/String;
    ARETURN
   L1
    MAXSTACK = 1
    MAXLOCALS = 0

  // access flags 0x19
  public final static test()Ljava/lang/String;
  @Lorg/jetbrains/annotations/NotNull;() // invisible
   L0
    LINENUMBER 5 L0
    GETSTATIC SimpleFunctionKt.x : Ljava/lang/String;
    ARETURN
   L1
    MAXSTACK = 1
    MAXLOCALS = 0

  // access flags 0x8
  static <clinit>()V
   L0
    LINENUMBER 3 L0
    INVOKESTATIC SimpleFunctionKt.foo ()Ljava/lang/Object;
    DUP
    IFNONNULL L1
    INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwNpe ()V
   L1
    CHECKCAST java/lang/String
    PUTSTATIC SimpleFunctionKt.x : Ljava/lang/String;
    RETURN
    MAXSTACK = 2
    MAXLOCALS = 0

  @Lkotlin/Metadata;(mv={1, 1, 15}, bv={1, 0, 3}, k=2, d1={"\u0000\n\n\u0000\n\u0002\u0010\u000e\n\u0002\u0008\u0007\u001a\u0011\u0010\u0004\u001a\u0002H\u0005\"\u0004\u0008\u0000\u0010\u0005\u00a2\u0006\u0002\u0010\u0006\u001a\u0006\u0010\u0007\u001a\u00020\u0001\"\u0011\u0010\u0000\u001a\u00020\u0001\u00a2\u0006\u0008\n\u0000\u001a\u0004\u0008\u0002\u0010\u0003\u00a8\u0006\u0008"}, d2={"x", "", "getX", "()Ljava/lang/String;", "foo", "T", "()Ljava/lang/Object;", "test", "test-module"})
  // compiled from: simpleFunction.kt
}
@META-INF/test-module.kotlin_module
<package <root>: [SimpleFunctionKt]>

 */