package io.svechnikov.tjgram.base.di.databinding

import javax.inject.Scope

/**
 * Identifies a type that the injector only instantiates once. Not inherited.
 *
 * @see javax.inject.Scope @Scope
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class DataBindingScope