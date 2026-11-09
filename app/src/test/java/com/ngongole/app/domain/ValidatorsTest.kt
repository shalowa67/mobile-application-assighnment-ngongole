package com.ngongole.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun phone_acceptsCommonZambianFormats() {
        assertEquals("+260971234567", Validators.normalisePhone("0971234567"))
        assertEquals("+260961234567", Validators.normalisePhone("096 123 4567"))
        assertEquals("+260771234567", Validators.normalisePhone("+260 77 123 4567"))
        assertEquals("+260951234567", Validators.normalisePhone("260951234567"))
        assertEquals("+260761234567", Validators.normalisePhone("076-123-4567"))
    }

    @Test
    fun phone_rejectsInvalidNumbers() {
        assertNull(Validators.normalisePhone("12345"))
        assertNull(Validators.normalisePhone("0211234567"))   // landline
        assertNull(Validators.normalisePhone("0981234567"))   // no 098 network
        assertNull(Validators.normalisePhone("09712345678"))  // too long
        assertNull(Validators.normalisePhone("abc"))
        assertNotNull(Validators.validatePhone(""))
    }

    @Test
    fun amount_rules() {
        assertNull(Validators.validateAmount("50"))
        assertNull(Validators.validateAmount("1,250.50"))
        assertNotNull(Validators.validateAmount(""))
        assertNotNull(Validators.validateAmount("0"))
        assertNotNull(Validators.validateAmount("-5"))
        assertNotNull(Validators.validateAmount("12.345"))
        assertNotNull(Validators.validateAmount("abc"))
        assertNotNull(Validators.validateAmount("200", maxNgwee = 10_000))
    }

    @Test
    fun name_rules() {
        assertNull(Validators.validateName("Mwila Banda"))
        assertNotNull(Validators.validateName("   "))
        assertNotNull(Validators.validateName("M"))
    }

    @Test
    fun money_parseAndFormat() {
        assertEquals(5000L, Money.parseToNgwee("50"))
        assertEquals(5050L, Money.parseToNgwee("50.5"))
        assertEquals(5005L, Money.parseToNgwee("50.05"))
        assertEquals(50L, Money.parseToNgwee(".5"))
        assertNull(Money.parseToNgwee("1.2.3"))
        assertEquals("K 1,250.50", Money.format(125050))
        assertEquals("K 0.00", Money.format(0))
    }
}
