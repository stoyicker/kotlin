package foobar

import bar.baz
import d0.AnotherSupertype

fun expectAnotherSupertype(x: AnotherSupertype) {
    x.hashCode()
}

fun main() {
    baz().foo()
    // TODO: support subtyping too
    expectAnotherSupertype(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Unit but AnotherSupertype was expected">baz().foo()</error>)
}
