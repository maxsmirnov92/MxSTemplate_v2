package net.maxsmr.core.network


import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

internal val EMPTY_TYPE_ARRAY: Array<Type> = arrayOf()

internal class ParameterizedTypeImpl private constructor(
    private val ownerType: Type?,
    private val rawType: Type,
    val typeArguments: Array<Type>,
) : ParameterizedType {

    override fun getActualTypeArguments() = typeArguments.clone()

    override fun getRawType() = rawType

    override fun getOwnerType() = ownerType

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun equals(other: Any?) =
        other is ParameterizedType && equals(this, other as ParameterizedType?)

    override fun hashCode(): Int {
        return typeArguments.contentHashCode() xor rawType.hashCode() xor ownerType.hashCodeOrZero
    }

    override fun toString(): String {
        val result = StringBuilder(30 * (typeArguments.size + 1))
        result.append(rawType.typeToString())
        if (typeArguments.isEmpty()) {
            return result.toString()
        }
        result.append("<").append(typeArguments[0].typeToString())
        for (i in 1 until typeArguments.size) {
            result.append(", ").append(typeArguments[i].typeToString())
        }
        return result.append(">").toString()
    }

    companion object {

        operator fun invoke(
            ownerType: Type?,
            rawType: Type,
            vararg typeArguments: Type,
        ): ParameterizedTypeImpl {
            if (rawType is Class<*>) {
                val enclosingClass = rawType.enclosingClass
                require(enclosingClass == null) { "unexpected owner type for $rawType: null" }
            }
            @Suppress("UNCHECKED_CAST")
            val finalTypeArgs = typeArguments.clone() as Array<Type>
            for (t in finalTypeArgs.indices) {
                finalTypeArgs[t].checkNotPrimitive()
                finalTypeArgs[t] = finalTypeArgs[t].canonicalize()
            }
            return ParameterizedTypeImpl(ownerType?.canonicalize(), rawType.canonicalize(), finalTypeArgs)
        }
    }
}

internal class GenericArrayTypeImpl private constructor(private val componentType: Type) : GenericArrayType {

    override fun getGenericComponentType() = componentType

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun equals(other: Any?) =
        other is GenericArrayType && equals(this, other as GenericArrayType?)

    override fun hashCode() = componentType.hashCode()

    override fun toString() = componentType.typeToString() + "[]"

    companion object {

        operator fun invoke(componentType: Type): GenericArrayTypeImpl {
            return GenericArrayTypeImpl(componentType.canonicalize())
        }
    }
}


internal class WildcardTypeImpl private constructor(
    private val upperBound: Type,
    private val lowerBound: Type?,
) : WildcardType {

    override fun getUpperBounds() = arrayOf(upperBound)

    override fun getLowerBounds() = lowerBound?.let { arrayOf(it) } ?: EMPTY_TYPE_ARRAY

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun equals(other: Any?) = other is WildcardType && equals(this, other as WildcardType?)

    override fun hashCode(): Int {
        return (if (lowerBound != null) 31 + lowerBound.hashCode() else 1) xor 31 + upperBound.hashCode()
    }

    override fun toString(): String {
        return when {
            lowerBound != null -> "? super ${lowerBound.typeToString()}"
            upperBound === Any::class.java -> "?"
            else -> "? extends ${upperBound.typeToString()}"
        }
    }

    companion object {

        operator fun invoke(
            upperBounds: Array<Type>,
            lowerBounds: Array<Type>,
        ): WildcardTypeImpl {
            require(lowerBounds.size <= 1)
            require(upperBounds.size == 1)
            return if (lowerBounds.size == 1) {
                lowerBounds[0].checkNotPrimitive()
                require(!(upperBounds[0] !== Any::class.java))
                WildcardTypeImpl(
                    lowerBound = lowerBounds[0].canonicalize(),
                    upperBound = Any::class.java
                )
            } else {
                upperBounds[0].checkNotPrimitive()
                WildcardTypeImpl(
                    lowerBound = null,
                    upperBound = upperBounds[0].canonicalize()
                )
            }
        }
    }
}

internal val Any?.hashCodeOrZero: Int
    get() {
        return this?.hashCode() ?: 0
    }

internal fun Type.typeToString(): String {
    return if (this is Class<*>) name else toString()
}

internal fun Type.canonicalize(): Type {
    return when (this) {
        is Class<*> -> {
            if (isArray) GenericArrayTypeImpl(this@canonicalize.componentType.canonicalize()) else this
        }
        is ParameterizedType -> {
            if (this is ParameterizedTypeImpl) return this
            ParameterizedTypeImpl(ownerType, rawType, *actualTypeArguments)
        }
        is GenericArrayType -> {
            if (this is GenericArrayTypeImpl) return this
            GenericArrayTypeImpl(genericComponentType)
        }
        is WildcardType -> {
            if (this is WildcardTypeImpl) return this
            WildcardTypeImpl(upperBounds, lowerBounds)
        }
        else -> this
    }
}

internal fun Type.checkNotPrimitive() {
    require(!(this is Class<*> && isPrimitive)) { "Unexpected primitive $this. Use the boxed type." }
}

internal fun equals(a: Type?, b: Type?): Boolean {
    if (a === b) {
        return true
    }

    when (a) {
        is Class<*> -> {
            return if (b is GenericArrayType) {
                equals(a.componentType, b.genericComponentType)
            } else {
                a == b
            }
        }
        is ParameterizedType -> {
            if (b !is ParameterizedType) return false
            val aTypeArguments = if (a is ParameterizedTypeImpl) a.typeArguments else a.actualTypeArguments
            val bTypeArguments = if (b is ParameterizedTypeImpl) b.typeArguments else b.actualTypeArguments
            return (
                    equals(a.ownerType, b.ownerType) &&
                            (a.rawType == b.rawType) && aTypeArguments.contentEquals(bTypeArguments)
                    )
        }
        is GenericArrayType -> {
            if (b is Class<*>) {
                return equals(b.componentType, a.genericComponentType)
            }
            if (b !is GenericArrayType) return false
            return equals(a.genericComponentType, b.genericComponentType)
        }
        is WildcardType -> {
            if (b !is WildcardType) return false
            return (a.upperBounds.contentEquals(b.upperBounds) && a.lowerBounds.contentEquals(b.lowerBounds))
        }
        is TypeVariable<*> -> {
            if (b !is TypeVariable<*>) return false
            return (a.genericDeclaration === b.genericDeclaration && (a.name == b.name))
        }
        else -> return false
    }
}