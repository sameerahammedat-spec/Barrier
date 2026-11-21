package com.rushgames.barrier.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Reports malformed Room values without modifying the original stored database text. */
object StoredDataRecoveryReporter {
    private val mutableAffectedValues = MutableStateFlow<Set<String>>(emptySet())

    val affectedValues: StateFlow<Set<String>> = mutableAffectedValues.asStateFlow()
    val hasCorruption: Boolean
        get() = mutableAffectedValues.value.isNotEmpty()

    fun report(fieldCategory: String) {
        mutableAffectedValues.update { current -> current + fieldCategory }
    }

    fun acknowledgeAfterRepair() {
        mutableAffectedValues.value = emptySet()
    }
}
