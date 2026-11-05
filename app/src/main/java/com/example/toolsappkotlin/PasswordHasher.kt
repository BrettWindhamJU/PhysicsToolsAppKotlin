package com.example.toolsappkotlin

import com.example.toolsappkotlin.data.User
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    // PBKDF2-HMAC-SHA1 is available on the project's minimum Android API 24.
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 1_300_000

    fun create(username: String, password: CharArray, role: String): User {
        require(role == "Student" || role == "Teacher")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return User(username, role, salt, derive(password, salt, ITERATIONS, ALGORITHM), ITERATIONS, ALGORITHM)
    }

    fun verify(password: CharArray, user: User): Boolean = MessageDigest.isEqual(
        user.passwordHash, derive(password, user.salt, user.iterations, user.algorithm))

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try { SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
