
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.Hex;
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

        byte[] data = key.getData();
        String hex = Hex.encode(data);
        Log.info("pub: " + hex);
        String expected = "04f365dd8abe7c3671ffbd2b9d68e43682c1ecd7d9fb71f7d250c87388d9d48d63b88c79820da334d4847f8a1bde9e9d05fdaca7d6ffb353d2be7b9db8ed86c35a";
        expected = expected.substring(2, hex.length());
        Assert.assertEquals(expected, hex.substring(2));
    }

    @Test
    public void testPrivateKey() throws ClassNotFoundException {
        PrivateKey sk = getPrivateKey();
        Log.info("ECC private key: " + sk);

        byte[] data = sk.getData();
        String hex = Hex.encode(data);
        Log.info("priv: " + hex);
        String expected = "39498636ab19f03bca97b78f1d4dbc06970f4a1715d2c4a03a34d8dae9a132a9";
        Assert.assertEquals(expected, hex);

        PublicKey pk = sk.getPublicKey();
        Log.info("ECC public key: " + pk);

        String text = "moky9527";
        byte[] plaintext = UTF8.encode(text);
        byte[] signature = sk.sign(plaintext);
        Log.info("ECC signature(\"" + text + "\") = " + Utils.hexEncode(signature));

        boolean ok = pk.verify(plaintext, signature);
        Assert.assertTrue(ok);
    }

    @Test
    public void testSignature() throws ClassNotFoundException {
        String secret = "c6e193266883a500c6e51a117e012d96ad113d5f21f42b28eb648be92a78f92f";
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "ECC");
        dictionary.put("data", secret);
        PrivateKey sKey = PrivateKey.getInstance(dictionary);

        byte[] data = "hello".getBytes();
        byte[] signature = sKey.sign(data);
        String res = Hex.encode(signature);
        Log.info("signature(hello) = " + res);

        PublicKey pKey = sKey.getPublicKey();
        Log.info("ECC public key: " + pKey);
        Log.info("pub data: " + Hex.encode(pKey.getData()));

        boolean ok = pKey.verify(data, signature);
        Assert.assertTrue(ok);

        String exp = "3045022100a314a579fb9f30a804c172eec4881ed603e661eed692797149dfdbce24d671d202203ccfab0603ad97c34864caa22d42a24d0cb5750fcb159476b8ae30a11edc0ed6";
        byte[] signature2 = Hex.decode(exp);
        ok = pKey.verify(data, signature2);
        Assert.assertTrue(ok);

        String pub = "0314bf901a6640033ea07b39c6b3acb675fc0af6a6ab526f378216085a93e5c7a2";
        dictionary.put("data", pub);
        pKey = PublicKey.getInstance(dictionary);
        Log.info("ECC public key: " + pKey);
        Log.info("pub data: " + Hex.encode(pKey.getData()));

        ok = pKey.verify(data, signature);
        Assert.assertTrue(ok);

        ok = pKey.verify(data, signature2);
        Assert.assertTrue(ok);
    }
}

// openssl ecparam -name secp256k1 -genkey -out secp256k1-priv.pem
// openssl ecparam -name secp256k1 -genkey -noout -out secp256k1-priv.pem

// openssl pkey -in secp256k1-priv.pem -text

// openssl ec -in secp256k1-priv.pem -pubout -out secp256k1-pub.pem

// openssl pkcs8 -topk8 -nocrypt -in secp256k1-priv.pem -out secp256k1-priv-pk8.pem
