package net.nonylene.photolinkviewer.tool

import java.math.BigInteger

object Base58 {
    private val CHARACTERS = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"
    private val CHARACTERS_LENGTH = BigInteger.valueOf(58)

    fun decode(string: String): String {
        var result = BigInteger.valueOf(0)
        for (i in 0..string.length() - 1) {
            val alpha = BigInteger.valueOf(CHARACTERS.indexOf(string.reversed().get(i)).toLong())
            result = result.add(alpha.multiply(CHARACTERS_LENGTH.pow(i)))
        }
        return result.toString()
    }
}
