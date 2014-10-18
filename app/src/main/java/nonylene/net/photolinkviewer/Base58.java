package nonylene.net.photolinkviewer;

public class Base58 {
    private static final String alphabet = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

    public static String decode(String string) {
        long result = 0;
        int piyo = string.length();
        for (int i = 0; i < piyo; i++) {
            long pow = (long) Math.pow(58, i);
            result += pow * (alphabet.indexOf(string.substring(piyo - i - 1, piyo - i)));
        }
        return String.valueOf(result);
    }
}
