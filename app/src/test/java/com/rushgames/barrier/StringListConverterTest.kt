package com.rushgames.barrier

import com.rushgames.barrier.data.model.StringListConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class StringListConverterTest {
    private val converter = StringListConverter()

    @Test
    fun readsLegacyPipeSeparatedValues() {
        assertEquals(listOf("one", "two"), converter.fromString("one|two"))
    }

    @Test
    fun roundTripsValuesContainingDelimiterUnicodeAndEmptyString() {
        val values = listOf("host|with-pipe.example", "mañana.example", "")

        assertEquals(values, converter.fromString(converter.toString(values)))
    }

    @Test
    fun roundTripsEmptyList() {
        assertEquals(emptyList<String>(), converter.fromString(converter.toString(emptyList())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCorruptVersionedValueInsteadOfSilentlyReplacingIt() {
        converter.fromString("json:{")
    }
}
