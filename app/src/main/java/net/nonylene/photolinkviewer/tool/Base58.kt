package net.nonylene.photolinkviewer.tool

import java.math.BigInteger

object Base58 {
    private val CHARACTERS = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"
    private val CHARACTERS_LENGTH = BigInteger.valueOf(58)

    fun decode(encoded: String): String {
        return encoded.reversed().withIndex().fold(BigInteger.valueOf(0)) { result, iv ->
            val alpha = BigInteger.valueOf(CHARACTERS.indexOf(iv.value).toLong())
            result.add(alpha.multiply(CHARACTERS_LENGTH.pow(iv.index)))
        }.toString()
    }
}
