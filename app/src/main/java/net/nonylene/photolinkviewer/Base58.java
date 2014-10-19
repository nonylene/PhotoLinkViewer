package net.nonylene.photolinkviewer;

import java.math.BigInteger;

public class Base58 {
    private static final String alphabet = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    public static String decode(String string) {
        BigInteger result = BigInteger.valueOf(0);
        int piyo = string.length();
        BigInteger len = BigInteger.valueOf(58);
        for (int i = 0; i < piyo; i++) {
            BigInteger alpha = BigInteger.valueOf(alphabet.indexOf(string.substring(piyo - i - 1, piyo - i)));
            alpha = alpha.multiply(len.pow(i));
            result = result.add(alpha);
        }
        return String.valueOf(result);
    }
}
