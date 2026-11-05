package com.example.toolsappkotlin

import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {
    @Test fun uniqueSaltsAndPasswordVerification() {
        val password = "a long test password".toCharArray()
        val first = PasswordHasher.create("alice", password, "Student")
        val second = PasswordHasher.create("bob", password, "Teacher")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.passwordHash.contentEquals(second.passwordHash))
        assertTrue(PasswordHasher.verify(password, first))
        assertFalse(PasswordHasher.verify("incorrect password".toCharArray(), first))
        assertEquals("Student", first.role)
        assertEquals("Teacher", second.role)
        assertEquals(16, first.salt.size)
        assertEquals(32, first.passwordHash.size)
    }
}
