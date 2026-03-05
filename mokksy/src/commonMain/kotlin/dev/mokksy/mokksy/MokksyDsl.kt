package dev.mokksy.mokksy

/**
 * DSL marker for the Mokksy stub-definition DSL.
 *
 * Prevents implicit access to outer DSL receiver scopes inside nested blocks.
 * For example, [dev.mokksy.mokksy.request.RequestSpecificationBuilder] methods
 * cannot be called from inside a `respondsWith { }` lambda, and vice versa.
 */
@DslMarker
public annotation class MokksyDsl
