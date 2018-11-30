/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.KotlinIr
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.webWorkers.moveWorkersToSeparateFiles
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    immediateDependencies: List<KlibModuleRef>,
    allDependencies: List<KlibModuleRef>,
    directory: String
): String {
    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, files, configuration, immediateDependencies, allDependencies)

    val moduleDescriptor = moduleFragment.descriptor

    val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, configuration)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        ExternalDependenciesGenerator(
            it.descriptor,
            symbolTable,
            irBuiltIns,
            deserializer = deserializer
        ).generateUnboundSymbolsAsDependencies()
    }

    fun compile(files: List<IrFile>): String {
        moduleFragment.files.clear()
        moduleFragment.files += files

        // Create stubs
        ExternalDependenciesGenerator(
            moduleDescriptor = moduleDescriptor,
            symbolTable = symbolTable,
            irBuiltIns = irBuiltIns
        ).generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()

        jsPhases.invokeToplevel(phaseConfig, context, moduleFragment)

        return moduleFragment.accept(IrModuleToJsTransformer(context), null).toString()
    }

    moduleFragment.files.flatMap { moveWorkersToSeparateFiles(it, context) }.forEach { workerFile ->
        val output = File(directory + File.pathSeparator + workerFile.name)
        val content = compile(dependencyModules.flatMap { it.files } + workerFile)
        output.parentFile.mkdirs()
        output.writeText(content)
    }

    // TODO: check the order
    val irFiles = dependencyModules.flatMap { it.files } + moduleFragment.files

    return compile(irFiles)
}