package dev.mokksy.mokksy

import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.TYPEALIAS
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * DSL marker for the Mokksy stub-definition DSL.
 *
 * Prevents implicit access to outer DSL receiver scopes inside nested blocks.
 * For example, [dev.mokksy.mokksy.request.RequestSpecificationBuilder] methods
 * cannot be called from inside a `respondsWith { }` lambda, and vice versa.
 */
@DslMarker
public annotation class MokksyDsl

/**
 * This annotation marks the Mokksy API that is considered experimental and is not subject to the
 * general compatibility guarantees. The behaviour of such API may change,
 * or the API may be removed completely in any further release.
 *
 * Any usage of a declaration annotated with [ExperimentalMokksyApi] must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalMokksyApi::class)`,
 * or by using the compiler argument `-opt-in=dev.mokksy.mokksy.ExperimentalMokksyApi`.
 */
@RequiresOptIn(
    message = "This API is experimental. It may be changed in the future without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS,
)
@MustBeDocumented
public annotation class ExperimentalMokksyApi

/**
 * API marked with this annotation is internal, and it is not intended to be used outside Mokksy.
 * It could be modified or removed without any notice.
 * Using it outside Mokksy could cause undefined behaviour and/or any unexpected effects.
 *
 * Example:
 * ```kotlin
 * @OptIn(InternalMokksyApi::class)
 * internal fun wireInternalFeature() { /* ... */ }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "This API is internal in Mokksy and should not be used. It could be removed or changed without notice.",
)
@Target(
    CLASS,
    TYPEALIAS,
    FUNCTION,
    PROPERTY,
    FIELD,
    CONSTRUCTOR,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class InternalMokksyApi
