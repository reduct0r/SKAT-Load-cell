package com.h2grow.skat_load_cell.domain.model

data class CommandResult(
    val ok: Boolean,
    val rawJson: String,
    val error: String? = null,
    val cmd: String? = null,
)