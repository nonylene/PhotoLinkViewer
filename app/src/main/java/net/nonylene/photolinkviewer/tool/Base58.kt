package net.nonylene.photolinkviewer.tool

import java.math.BigInteger

object Base58 {
    private val CHARACTERS = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"
    private val CHARACTERS_LENGTH = BigInteger.valueOf(58)

    fun decode(encoded: String): String {
        val reversed = encoded.reversed()
        return (0..reversed.length() - 1).fold(BigInteger.valueOf(0)) { result, i ->
            val alpha = BigInteger.valueOf(CHARACTERS.indexOf(reversed.get(i)).toLong())
            result.add(alpha.multiply(CHARACTERS_LENGTH.pow(i)))
        }.toString()
    }
}
