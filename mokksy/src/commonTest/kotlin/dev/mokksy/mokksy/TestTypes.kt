package dev.mokksy.mokksy

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord

@Serializable
@JvmRecord
data class Input(
    val name: String,
    val age: Int? = null,
)

@Serializable
data class Output(
    val result: String,
)

@Serializable
data class OutputChunk(
    val item: String,
)
