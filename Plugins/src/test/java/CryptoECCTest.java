
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Address;
import chat.dim.digest.RIPEMD160;
import chat.dim.digest.SHA256;
import chat.dim.mkm.plugins.BTCAddress;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.Hex;

public class CryptoECCTest {

    private void testKeys(PrivateKey sKey, PublicKey pKey) {
        Log.info("ECC private key: " + sKey);
        Log.info("secret data: " + Hex.encode(sKey.getData()));

        Log.info("ECC public key: " + pKey);
        Log.info("pub data: " + Hex.encode(pKey.getData()));

        // sign
        byte[] data = "moky".getBytes();
        byte[] signature = sKey.sign(data);
        String res = Hex.encode(signature);
        Log.info("signature(moky) = " + res);

        // verify
        boolean ok = pKey.verify(data, signature);
        Assert.assertTrue(ok);

        pKey = sKey.getPublicKey();
        ok = pKey.verify(data, signature);
        Assert.assertTrue(ok);
    }

    private PrivateKey testKeys(String secret, String pub) throws ClassNotFoundException {
        // create private key
        Map<String, Object> dict1 = new HashMap<>();
        dict1.put("algorithm", "ECC");
        dict1.put("data", secret);
        PrivateKey sKey = PrivateKey.getInstance(dict1);

        // create public key
        Map<String, Object> dict2 = new HashMap<>();
        dict2.put("algorithm", "ECC");
        dict2.put("data", pub);
        PublicKey pKey = PublicKey.getInstance(dict2);

        testKeys(sKey, pKey);

        return sKey;
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
    public void testECCPublicKey() throws ClassNotFoundException {
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
    public void testECCWithPEM() throws ClassNotFoundException {
        Log.info("-------- test PEM --------");
        PrivateKey sKey = getPrivateKey();
        Log.info("ECC private key: " + sKey);

        byte[] data = sKey.getData();
        String hex = Hex.encode(data);
        Log.info("priv: " + hex);
        String expected = "39498636ab19f03bca97b78f1d4dbc06970f4a1715d2c4a03a34d8dae9a132a9";
        Assert.assertEquals(expected, hex);

        PublicKey pKey = sKey.getPublicKey();
        testKeys(sKey, pKey);

        pKey = getPublicKey();
        testKeys(sKey, pKey);
    }

    @Test
    public void testECCNewKeys() throws ClassNotFoundException {
        Log.info("-------- test new keys --------");
        PrivateKey sk = PrivateKey.generate(PrivateKey.ECC);
        PublicKey pk = sk.getPublicKey();
        testKeys(sk, pk);
    }

    @Test
    public void testECCWithHex() throws ClassNotFoundException {
        Log.info("-------- test HEX --------");
        PrivateKey sKey = testKeys("c6e193266883a500c6e51a117e012d96ad113d5f21f42b28eb648be92a78f92f",
                "0314bf901a6640033ea07b39c6b3acb675fc0af6a6ab526f378216085a93e5c7a2");

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
    }

    @Test
    public void testECCKeys() throws ClassNotFoundException {
        Log.info("-------- test keys --------");
        PrivateKey sKey = testKeys("de97fdbdb823a197603e1f2cb8b1bded3824147e88ebd47367ba82d4b5600d73",
                "047c91259636a5a16538e0603636f06c532dd6f2bb42f8dd33fa0cdb39546cf449612f3eaf15db9443b7e0668ef22187de9059633eb23112643a38771c630db911");
        PublicKey pKey = sKey.getPublicKey();

        byte[] pub = pKey.getData();
        Log.info("pub: " + Hex.encode(pub));

        pub = Hex.decode("04d061e9c5891f579fd548cfd22ff29f5c642714cc7e7a9215f0071ef5a5723f691757b28e31be71f09f24673eed52348e58d53bcfd26f4d96ec6bf1489eab429d");

        byte[] data = RIPEMD160.digest(SHA256.digest(pub));
        Log.info("hash: " + Hex.encode(data));

        Address address = BTCAddress.generate(pub, (byte) 0);
        Log.info("address: " + address);
    }
}

// openssl ecparam -name secp256k1 -genkey -out secp256k1-priv.pem
// openssl ecparam -name secp256k1 -genkey -noout -out secp256k1-priv.pem

// openssl pkey -in secp256k1-priv.pem -text

// openssl ec -in secp256k1-priv.pem -pubout -out secp256k1-pub.pem

// openssl pkcs8 -topk8 -nocrypt -in secp256k1-priv.pem -out secp256k1-priv-pk8.pem
