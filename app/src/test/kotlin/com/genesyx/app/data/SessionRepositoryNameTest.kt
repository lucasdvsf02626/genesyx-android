package com.genesyx.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Home greeting is the first line of the app, so a raw email localpart there does not read as
 * a placeholder — it reads as her name spelled wrong. Accents have to survive the transform, which
 * is why it touches the first character of each word and nothing else.
 */
class SessionRepositoryNameTest {

    @Test
    fun `an address becomes something that reads as a name`() {
        assertEquals("Ada", SessionRepository.nameFromAddress("ada@example.com"))
        assertEquals("Lucas Valença", SessionRepository.nameFromAddress("lucas.valença@example.com"))
        assertEquals("Ada Lovelace", SessionRepository.nameFromAddress("ada_lovelace@example.com"))
        assertEquals("Ada B Tag", SessionRepository.nameFromAddress("ada-b+tag@example.com"))
    }

    @Test
    fun `an already-capitalised localpart is left exactly as it is`() {
        assertEquals("Ada", SessionRepository.nameFromAddress("Ada@example.com"))
        assertEquals("McDonald", SessionRepository.nameFromAddress("McDonald@example.com"))
    }
}
