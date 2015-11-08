package net.nonylene.photolinkviewer.tool;

import android.util.Base64;
import android.util.Log;

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

// deprecated but exists for compatibility
public class Encryption {

    public static Key generate() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            keyGenerator.init(256, secureRandom);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e("keyerror", e.toString());
            return null;
        }
    }

    public static String encrypt(byte[] text, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(text), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("encrypt", e.toString());
            return null;
        }
    }

    public static String decrypt(byte[] code, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(code), "UTF-8");
        } catch (Exception e) {
            Log.e("decrypt", e.toString());
            return null;
        }
    }
}
