/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.types.KotlinType

fun KotlinType.refineTypeIfNeeded(
    moduleDescriptor: ModuleDescriptor,
    languageVersionSettings: LanguageVersionSettings
): KotlinType {
    if (!languageVersionSettings.getFlag(AnalysisFlags.useTypeRefinement)) return this

    return refine(moduleDescriptor)
}
