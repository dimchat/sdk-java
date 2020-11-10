
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.UTF8;

public class CryptoECCTest {

    @Test
    public void testECC() throws ClassNotFoundException {
        PrivateKey sk = PrivateKey.generate(PrivateKey.ECC);
        Log.info("ECC private key: " + sk);

        PublicKey pk = sk.getPublicKey();
        Log.info("ECC public key: " + pk);

        String text = "moky9527";
        byte[] plaintext = UTF8.encode(text);
        byte[] signature = sk.sign(plaintext);
        Log.info("ECC signature(\"" + text + "\") = " + Utils.hexEncode(signature));

        boolean ok = pk.verify(plaintext, signature);
        Assert.assertTrue(ok);
    }

    private static PublicKey getPublicKey() throws ClassNotFoundException {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "ECC");
        dictionary.put("data", "-----BEGIN PUBLIC KEY-----\n" +
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE82Xdir58NnH/vSudaOQ2gsHs19n7cffS\n" +
                "UMhziNnUjWO4jHmCDaM01IR/ihvenp0F/ayn1v+zU9K+e5247YbDWg==\n" +
                "-----END PUBLIC KEY-----");
        return PublicKey.getInstance(dictionary);
    }

    private static PrivateKey getPrivateKey() throws ClassNotFoundException {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "ECC");
        dictionary.put("data", "-----BEGIN PRIVATE KEY-----\n" +
                "MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgOUmGNqsZ8DvKl7ePHU28\n" +
                "BpcPShcV0sSgOjTY2umhMqmhRANCAATzZd2Kvnw2cf+9K51o5DaCwezX2ftx99JQ\n" +
                "yHOI2dSNY7iMeYINozTUhH+KG96enQX9rKfW/7NT0r57nbjthsNa\n" +
                "-----END PRIVATE KEY-----");
        return PrivateKey.getInstance(dictionary);
    }

    @Test
    public void testPublicKey() throws ClassNotFoundException {
        PublicKey key = getPublicKey();
        Log.info("ECC public key: " + key);
    }

    @Test
    public void testPrivateKey() throws ClassNotFoundException {
        PrivateKey sk = getPrivateKey();
        Log.info("ECC private key: " + sk);

        PublicKey pk = sk.getPublicKey();
        Log.info("ECC public key: " + pk);


        String text = "moky9527";
        byte[] plaintext = UTF8.encode(text);
        byte[] signature = sk.sign(plaintext);
        Log.info("ECC signature(\"" + text + "\") = " + Utils.hexEncode(signature));

        boolean ok = pk.verify(plaintext, signature);
        Assert.assertTrue(ok);
    }
}

// openssl ecparam -name secp256k1 -genkey -out secp256k1-priv.pem
// openssl ecparam -name secp256k1 -genkey -noout -out secp256k1-priv.pem

// openssl pkey -in secp256k1-priv.pem -text

// openssl ec -in secp256k1-priv.pem -pubout -out secp256k1-pub.pem

// openssl pkcs8 -topk8 -nocrypt -in secp256k1-priv.pem -out secp256k1-priv-pk8.pem
