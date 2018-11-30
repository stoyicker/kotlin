/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.webWorkers

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.sure

fun moveWorkersToSeparateFiles(irFile: IrFile, context: JsIrBackendContext): List<IrFile> {
    val workerCalls = irFile.filterCalls { it.descriptor.isWorker() }

    if (workerCalls.isEmpty()) return emptyList()

    val workerLambdas = workerCalls
        .map { it.getValueArgument(0) as? IrBlock ?: error("worker intrinsic accepts only block, but got $it") }
        .map { it.statements.filterIsInstance<IrFunctionReference>().single().symbol.owner }

    for (workerLambda in workerLambdas) {
        replaceReturnsWithPostMessage(workerLambda, context)
        moveToSeparateFile(workerLambda, context, irFile)
        workerLambda.dump()
    }

    replaceWorkerIntrinsicCalls(workerCalls, irFile, context)
    return emptyList()
}

fun moveToSeparateFile(workerLambda: IrFunction, context: JsIrBackendContext, irFile: IrFile) {
}

private fun replaceReturnsWithPostMessage(workerLambda: IrFunction, context: JsIrBackendContext) {
    val postMessage = context.ir.symbols.cast<JsIrBackendContext.JsSymbols>().postMessage

    workerLambda.body.sure { "worker lambda $workerLambda shall have body" }.transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            val visited = super.visitReturn(expression) as IrReturn
            return newCallWithUndefinedOffsets(
                postMessage,
                listOf(visited.value)
            )
        }
    })
}

private fun replaceWorkerIntrinsicCalls(
    workerCalls: List<IrCall>,
    irFile: IrFile,
    context: JsIrBackendContext
) {
    val callToName = workerCalls.zip((0 until workerCalls.size).map { "__${irFile.name}_Worker_$it.js" }).toMap()

    val workerClass = context.ir.symbols.cast<JsIrBackendContext.JsSymbols>().workerClass

    irFile.transformCalls { call ->
        if (call in workerCalls) {
            val constructor = workerClass.constructors.first()
            newCallWithReusedOffsets(
                call,
                symbol = constructor,
                arguments = listOf(
                    newCallWithUndefinedOffsets(
                        symbol = context.jsCodeSymbol,
                        arguments = listOf(context.string("new Worker(\"${callToName[call]}\")"))
                    )
                )
            )
        } else call
    }
}

fun IrFile.filterCalls(predicate: (IrCall) -> Boolean): List<IrCall> {
    val res = arrayListOf<IrCall>()
    acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitCall(expression: IrCall) {
            if (predicate(expression)) {
                res += expression
            }
            super.visitCall(expression)
        }
    })
    return res
}

fun IrFile.transformCalls(transformer: (IrCall) -> IrCall) {
    transformChildrenVoid(object : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            val visited = super.visitCall(expression) as IrCall
            return transformer(visited)
        }
    })
}

fun newCallWithReusedOffsets(
    call: IrCall,
    symbol: IrFunctionSymbol,
    arguments: List<IrExpression?>
) = IrCallImpl(call.startOffset, call.endOffset, symbol.owner.returnType, symbol).apply {
    for ((index, expression) in arguments.withIndex()) {
        putValueArgument(index, expression)
    }
}

fun newCallWithUndefinedOffsets(
    symbol: IrFunctionSymbol,
    arguments: List<IrExpression?>
) = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.returnType, symbol).apply {
    for ((index, expression) in arguments.withIndex()) {
        putValueArgument(index, expression)
    }
}

private fun FunctionDescriptor.isWorker(): Boolean = isTopLevelInPackage("worker", "kotlin.worker")

private fun JsIrBackendContext.string(s: String) = JsIrBuilder.buildString(irBuiltIns.stringType, s)
private val JsIrBackendContext.jsCodeSymbol
    get() = symbolTable.referenceSimpleFunction(getJsInternalFunction("js"))

