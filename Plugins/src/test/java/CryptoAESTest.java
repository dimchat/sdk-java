
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;

public class CryptoAESTest {

    @Test
    public void testAES() throws UnsupportedEncodingException, ClassNotFoundException {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "AES");
        dictionary.put("data", "C2+xGizLL1G1+z9QLPYNdp/bPP/seDvNw45SXPAvQqk=");
        dictionary.put("iv", "SxPwi6u4+ZLXLdAFJezvSQ==");

        SymmetricKey key = SymmetricKey.getInstance(dictionary);
        Log.info("key: " + key);
        String json = JSON.encode(key);
        Log.info("JSON(" + json.length() + " bytes): " + json);
        String algor = (String) key.get("A");
        if (algor == null) {
            algor = (String) key.get("algorithm");
        }
        Log.info("A: " + algor);

        String text;
        byte[] plaintext;
        byte[] ciphertext;
        byte[] data;
        String decrypt;

        text = "moky";
        plaintext = text.getBytes("UTF-8");
        ciphertext = key.encrypt(plaintext);
        Log.info("encrypt(\"" + text + "\") = " + Utils.hexEncode(ciphertext));
        Log.info("encrypt(\"" + text + "\") = " + Base64.encode(ciphertext));

        data = key.decrypt(ciphertext);
        decrypt = new String(data, "UTF-8");
        Log.info("decrypt to " + decrypt);
        Log.info(text + " -> " + Base64.encode(ciphertext) + " -> " + decrypt);

        Assert.assertEquals("0xtbqZN6x2aWTZn0DpCoCA==", Base64.encode(ciphertext));

        SymmetricKey key2 = SymmetricKey.getInstance(dictionary);
        Log.info("key2: " + key2);
        Assert.assertEquals(key, key2);
//        Assert.assertTrue(key.equals(key2));

//        text = "XX5qfromb3R078VVK7LwVA=="; // NoPadding
        text = "0xtbqZN6x2aWTZn0DpCoCA==";
        ciphertext = Base64.decode(text);
        plaintext = key2.decrypt(ciphertext);
        Log.info("FIXED: " + text + " -> " + (plaintext == null ? null : new String(plaintext)));
//        plaintext = key2.decrypt(ciphertext);
//        log("FIXED: " + text + " -> " + (plaintext == null ? null : new String(plaintext)));

        // random key
        key = SymmetricKey.generate(SymmetricKey.AES);
        Log.info("key: " + key);

        text = "moky";
        plaintext = text.getBytes("UTF-8");
        ciphertext = key.encrypt(plaintext);
        Log.info("encrypt(\"" + text + "\") = " + Utils.hexEncode(ciphertext));
        Log.info("encrypt(\"" + text + "\") = " + Base64.encode(ciphertext));

        data = key.decrypt(ciphertext);
        decrypt = new String(data, "UTF-8");
        Log.info("decrypt to " + decrypt);

        Assert.assertEquals(text, decrypt);
    }
}
