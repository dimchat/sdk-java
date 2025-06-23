
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.AsymmetricAlgorithms;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.digest.RIPEMD160;
import chat.dim.digest.SHA256;
import chat.dim.format.Hex;
import chat.dim.mkm.BTCAddress;
import chat.dim.mkm.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.Facebook;

public class CryptoECCTest {

    private static final Facebook facebook = SharedFacebook.getInstance();

    static {
        String javaLibraryPath = "/Users/moky/Documents/GitHub/dimchat/sdk-java/NativePlugins/libs";
        System.load(javaLibraryPath + "/libSecp256k1.so");
        //System.loadLibrary("Secp256k1");
    }

    private static PublicKey getPublicKey(String pem) {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "ECC");
        dictionary.put("data", pem);
        return PublicKey.parse(dictionary);
    }

    private static PrivateKey getPrivateKey(String pem) {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "ECC");
        dictionary.put("data", pem);
        return PrivateKey.parse(dictionary);
    }

    private static PublicKey getPublicKey() {
        return getPublicKey("-----BEGIN PUBLIC KEY-----\n" +
                "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE82Xdir58NnH/vSudaOQ2gsHs19n7cffS\n" +
                "UMhziNnUjWO4jHmCDaM01IR/ihvenp0F/ayn1v+zU9K+e5247YbDWg==\n" +
                "-----END PUBLIC KEY-----");
    }

    private static PrivateKey getPrivateKey() {
        return getPrivateKey("-----BEGIN PRIVATE KEY-----\n" +
                "MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgOUmGNqsZ8DvKl7ePHU28\n" +
                "BpcPShcV0sSgOjTY2umhMqmhRANCAATzZd2Kvnw2cf+9K51o5DaCwezX2ftx99JQ\n" +
                "yHOI2dSNY7iMeYINozTUhH+KG96enQX9rKfW/7NT0r57nbjthsNa\n" +
                "-----END PRIVATE KEY-----");
    }

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
        if (ok) {
            Log.info("keys match!");
        }
        Assert.assertTrue(ok);

        pKey = sKey.getPublicKey();
        ok = pKey.verify(data, signature);
        if (ok) {
            Log.info("private key OK!");
        }
        Assert.assertTrue(ok);
    }

    private PrivateKey testKeys(String secret, String pub) {
        PrivateKey sKey = getPrivateKey(secret);
        PublicKey pKey = pub == null ? sKey.getPublicKey() : getPublicKey(pub);
        testKeys(sKey, pKey);
        return sKey;
    }

    private void testAddress(PublicKey pKey) {
        byte[] pub = pKey.getData();
        Log.info("pub: " + Hex.encode(pub));

        BTCAddress btc = BTCAddress.generate(pub, (byte) 0);
        Log.info("BTC address: " + btc);

        ETHAddress eth = ETHAddress.generate(pub);
        Log.info("ETH address: " + eth);
    }

    @Test
    public void testECCPublicKey() {
        PublicKey key = getPublicKey();
        Log.info("ECC public key: " + key);

        byte[] data = key.getData();
        String hex = Hex.encode(data);
        Log.info("pub: " + hex);
        String expected = "04f365dd8abe7c3671ffbd2b9d68e43682c1ecd7d9fb71f7d250c87388d9d48d63b88c79820da334d4847f8a1bde9e9d05fdaca7d6ffb353d2be7b9db8ed86c35a";
        Assert.assertEquals(expected, hex);

        PrivateKey sKey = getPrivateKey("2d95856acf68e316092c145abed124418e6a5592b38cabe2a87d8403597424c4");
        key = sKey.getPublicKey();
        Log.info("public key: " + key);
    }

    @Test
    public void testECCWithPEM() {
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
    public void testECCNewKeys() {
        Log.info("-------- test new keys --------");
        PrivateKey sk = PrivateKey.generate(AsymmetricAlgorithms.ECC);
        PublicKey pk = sk.getPublicKey();
        testKeys(sk, pk);

//        sk = testKeys("-----BEGIN PRIVATE KEY-----\nMIGNAgEBMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgRwNa2FuE6W8Z3cCrzwTL\nm4vSbeSrqbhHVOHUcNosBVGgBwYFK4EEAAqhRANCAASc98Xix4Xh0TWqLdrvQPZz\n6/CKMA5NT4vtwNmHzTJt7u6ZcRKTZzg++PKl8oR8qAnOeMbpapAbPxphUeTEtOsQ\n-----END PRIVATE KEY-----",
//                "-----BEGIN PUBLIC KEY-----\nMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnPfF4seF4dE1qi3a70D2c+vwijAOTU+L\n7cDZh80ybe7umXESk2c4PvjypfKEfKgJznjG6WqQGz8aYVHkxLTrEA==\n-----END PUBLIC KEY-----");
        sk = testKeys("5ae4c458c584ab3b3c8b14c7462f295ed6c22d4d376ae625e9d0a93145c3345c",
                "04a34ba8e23e8abc035e238fb70920289d69e130c7779cf432005f0bfc9482282af496e9ae92ad3f7ff68932855d1d6d5bc30eb59dc0a6c579fa134c830ce14ce3");

//        sk = getPrivateKey("-----BEGIN PRIVATE KEY-----\nMIGNAgEBMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgWuTEWMWEqzs8ixTHRi8p\nXtbCLU03auYl6dCpMUXDNFygBwYFK4EEAAqhRANCAASjS6jiPoq8A14jj7cJICid\naeEwx3ec9DIAXwv8lIIoKvSW6a6SrT9/9okyhV0dbVvDDrWdwKbFefoTTIMM4Uzj\n-----END PRIVATE KEY-----");
//        sk = getPrivateKey("5ae4c458c584ab3b3c8b14c7462f295ed6c22d4d376ae625e9d0a93145c3345c");
        
//        sk = getPrivateKey("-----BEGIN PRIVATE KEY-----\n" +
//                "MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgRwNa2FuE6W8Z3cCrzwTL\n" +
//                "m4vSbeSrqbhHVOHUcNosBVGhRANCAASc98Xix4Xh0TWqLdrvQPZz6/CKMA5NT4vt\n" +
//                "wNmHzTJt7u6ZcRKTZzg++PKl8oR8qAnOeMbpapAbPxphUeTEtOsQ\n" +
//                "-----END PRIVATE KEY-----");

        byte[] pri = sk.getData();
        Log.info("pri data: " + Hex.encode(pri));

        pk = sk.getPublicKey();
        byte[] pub = pk.getData();
        Log.info("pub data: " + Hex.encode(pub));
        
        pk = getPublicKey("-----BEGIN PUBLIC KEY-----\nMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnPfF4seF4dE1qi3a70D2c+vwijAOTU+L\n7cDZh80ybe7umXESk2c4PvjypfKEfKgJznjG6WqQGz8aYVHkxLTrEA==\n-----END PUBLIC KEY-----");
        pub = pk.getData();
        Log.info("pub2data: " + Hex.encode(pub));
    }

    @Test
    public void testECCWithHex() {
        Log.info("-------- test HEX --------");
        PrivateKey sKey = testKeys("c6e193266883a500c6e51a117e012d96ad113d5f21f42b28eb648be92a78f92f",
                null/*"0314bf901a6640033ea07b39c6b3acb675fc0af6a6ab526f378216085a93e5c7a2"*/);

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
    public void testECCKeys() {
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

    @Test
    public void testWeb3j() {
        Log.info("-------- test web3j --------");
        PrivateKey sKey;
        PublicKey pKey;

        // Alice
        sKey = testKeys("a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6",
                "04506bc1dc099358e5137292f4efdd57e400f29ba5132aa5d12b18dac1c1f6aab" +
                        "a645c0b7b58158babbfa6c6cd5a48aa7340a8749176b120e8516216787a13dc76");
        pKey = sKey.getPublicKey();
        testAddress(pKey);

        // Bob
        sKey = testKeys("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4",
                "040947751e3022ecf3016be03ec77ab0ce3c2662b4843898cb068d74f698ccc8ad7"+
                        "5aa17564ae80a20bb044ee7a6d903e8e8df624b089c95d66a0570f051e5a05b");
        pKey = sKey.getPublicKey();
        testAddress(pKey);
    }

    private PrivateKey testAddress(String hex) {
        PrivateKey sKey = getPrivateKey(hex);
        PublicKey pKey = sKey.getPublicKey();
        testKeys(sKey, pKey);
        testAddress(pKey);
        return sKey;
    }

    @Test
    public void testETH() {
        Log.info("-------- test ETH --------");
        PrivateKey sKey;
        PublicKey pKey;

        sKey = testAddress("18e14a7b6a307f426a94f8114701e7c8e774e7f9a47e2c2035db29a206321725");
        // 16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM
        // 0x3E9003153d9A39D3f57B126b0c38513D5e289c3E
        pKey = sKey.getPublicKey();
        byte[] pub = pKey.getData();
        byte[] sig = sKey.sign(pub);
        String hex = Hex.encode(sig);
        boolean ok = pKey.verify(pub, sig);
        Log.info("signature: " + ok + " -> " + hex);

        sig = Hex.decode("304402203e723721984515805335142c6710cb74862fb63163fed091a7cf91d7f4226e93022001272261ae70bee6e6fbf0e6cb950f3a80fed3941d616432b2a19417fda285c4");
        ok = pKey.verify(pub, sig);

        testAddress("ccea9c5a20e2b78c2e0fbdd8ae2d2b67e6b1894ccb7a55fc1de08bd53994ea64");
        // 14xfJr1DArtYR156XBs28FoYk6sQqirT2s
        // 0x9156a7cdab767ffe161ed21a0cb0b688b545b01f

        testAddress("cdc251339ae7f5eb9307b2fabf99fa0c559f4a830d9775da750680aeb7736984");
        // compressed address: 1sBU8e7yewpdJzojPba3RqCYWZBXohVe1

        testAddress("de97fdbdb823a197603e1f2cb8b1bded3824147e88ebd47367ba82d4b5600d73");

        testAddress("11af9e9f87c53beedfe7eb3f1e9b6e2592b382ab3ecd83a92a6c20cb0c885f63");
        //
    }

    private static void testValidate(String address, String expected) {
        String validate = ETHAddress.getValidateAddress(address);
        Log.info("validate(" + address + "): " + validate);
        Assert.assertEquals(expected, validate);
    }

    @Test
    public void testValidateAddress() {
        boolean ok;
        Log.info("-------- test validate --------");

        ok = ETHAddress.isValidate("0xD4a16aa11Bd0D3315698792F5E1F66770F9Cd78F");
        Assert.assertTrue(ok);

        ok = ETHAddress.isValidate("0x40DAB7E81503AA1F8c1ef3574842017277755646");
        Assert.assertTrue(ok);

        ok = ETHAddress.isValidate("0x50352B904445576242444bc1924e93e61090738c");
        Assert.assertTrue(!ok);

        testValidate("0x50352B904445576242444bc1924e93e61090738c",
                "0x50352B904445576242444bc1924e93e61090738C");
        testValidate("0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359",
                "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359");
    }
}

// openssl ecparam -name secp256k1 -genkey -out secp256k1-priv.pem
// openssl ecparam -name secp256k1 -genkey -noout -out secp256k1-priv.pem

// openssl pkey -in secp256k1-priv.pem -text

// openssl ec -in secp256k1-priv.pem -pubout -out secp256k1-pub.pem

// openssl pkcs8 -topk8 -nocrypt -in secp256k1-priv.pem -out secp256k1-priv-pk8.pem
