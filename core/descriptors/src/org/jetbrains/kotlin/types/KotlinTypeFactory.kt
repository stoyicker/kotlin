/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object KotlinTypeFactory {
    private fun computeMemberScope(
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        moduleDescriptor: ModuleDescriptor? = null
    ): MemberScope {
        val basicDescriptor = constructor.declarationDescriptor
        val classId = basicDescriptor.safeAs<ClassifierDescriptorWithTypeParameters>()?.classId
        val descriptor =
            if (classId != null)
                moduleDescriptor?.findClassAcrossModuleDependencies(classId) ?: basicDescriptor
            else basicDescriptor

        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
            is ClassDescriptor -> {
                if (arguments.isEmpty())
                    descriptor.defaultType.memberScope
                else
                    descriptor.getMemberScope(TypeConstructorSubstitution.create(constructor, arguments))
            }
            is TypeAliasDescriptor -> ErrorUtils.createErrorScope("Scope for abbreviation: ${descriptor.name}", true)
            else -> throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
        }
    }

    @JvmStatic
    fun simpleType(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType {
        if (annotations.isEmpty() && arguments.isEmpty() && !nullable && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            annotations, constructor, arguments, nullable,
            computeMemberScope(constructor, arguments)
        ) { moduleDescriptor ->
            computeMemberScope(constructor, arguments, moduleDescriptor)
        }
    }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
        annotations: Annotations,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, { memberScope })
            .let {
                if (annotations.isEmpty())
                    it
                else
                    AnnotatedSimpleType(it, annotations)
            }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
            annotations: Annotations,
            constructor: TypeConstructor,
            arguments: List<TypeProjection>,
            nullable: Boolean,
            memberScope: MemberScope,
            scopeFactory: (ModuleDescriptor) -> MemberScope
    ): SimpleType =
            SimpleTypeImpl(constructor, arguments, nullable, memberScope, scopeFactory)
            .let {
                if (annotations.isEmpty())
                    it
                else
                    AnnotatedSimpleType(it, annotations)
            }

    @JvmStatic
    fun simpleNotNullType(
        annotations: Annotations,
        descriptor: ClassDescriptor,
        arguments: List<TypeProjection>
    ): SimpleType = simpleType(annotations, descriptor.typeConstructor, arguments, nullable = false)

    @JvmStatic
    fun simpleType(
        baseType: SimpleType,
        annotations: Annotations = baseType.annotations,
        constructor: TypeConstructor = baseType.constructor,
        arguments: List<TypeProjection> = baseType.arguments,
        nullable: Boolean = baseType.isMarkedNullable
    ): SimpleType = simpleType(annotations, constructor, arguments, nullable)

    @JvmStatic
    fun flexibleType(lowerBound: SimpleType, upperBound: SimpleType): UnwrappedType {
        if (lowerBound == upperBound) return lowerBound
        return FlexibleTypeImpl(lowerBound, upperBound)
    }

    @JvmStatic
    fun integerLiteralType(
        annotations: Annotations,
        constructor: IntegerLiteralTypeConstructor,
        nullable: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        annotations,
        constructor,
        emptyList(),
        nullable,
        ErrorUtils.createErrorScope("Scope for integer literal type", true)
    )
}

private class SimpleTypeImpl(
    override val constructor: TypeConstructor,
    override val arguments: List<TypeProjection>,
    override val isMarkedNullable: Boolean,
    override val memberScope: MemberScope,
        private val scopeFactory: (ModuleDescriptor) -> MemberScope
) : SimpleType() {
    override val annotations: Annotations get() = Annotations.EMPTY

    override fun replaceAnnotations(newAnnotations: Annotations) =
        if (newAnnotations.isEmpty())
            this
        else
            AnnotatedSimpleType(this, newAnnotations)

    override fun makeNullableAsSpecified(newNullability: Boolean) = when {
            newNullability == isMarkedNullable -> this
            newNullability -> NullableSimpleType(this)
            else -> NotNullSimpleType(this)
        }

    init {
        if (memberScope is ErrorUtils.ErrorScope) {
            throw IllegalStateException("SimpleTypeImpl should not be created for error type: $memberScope\n$constructor")
        }
    }

    override fun refine(moduleDescriptor: ModuleDescriptor): SimpleType {
        if (constructor.declarationDescriptor?.module === moduleDescriptor) return this

        return SimpleTypeImpl(constructor, arguments, isMarkedNullable, scopeFactory(moduleDescriptor), scopeFactory)
    }
}

abstract class DelegatingSimpleTypeImpl(override val delegate: SimpleType) : DelegatingSimpleType() {
    override fun replaceAnnotations(newAnnotations: Annotations) =
        if (newAnnotations !== annotations)
            AnnotatedSimpleType(this, newAnnotations)
        else
            this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        if (newNullability == isMarkedNullable) return this
        return delegate.makeNullableAsSpecified(newNullability).replaceAnnotations(annotations)
    }
}

private class AnnotatedSimpleType(
    delegate: SimpleType,
    override val annotations: Annotations
) : DelegatingSimpleTypeImpl(delegate) {
    override fun replaceDelegate(delegate: SimpleType) = AnnotatedSimpleType(delegate, annotations)
}

private class NullableSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedNullable: Boolean
        get() = true

    override fun replaceDelegate(delegate: SimpleType) = NullableSimpleType(delegate)
}

private class NotNullSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedNullable: Boolean
        get() = false

    override fun replaceDelegate(delegate: SimpleType) = NotNullSimpleType(delegate)
}
