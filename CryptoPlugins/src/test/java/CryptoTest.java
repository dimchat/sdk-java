
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.digest.Keccak256;
import chat.dim.digest.RIPEMD160;
import chat.dim.digest.SHA256;
import chat.dim.format.Base58;
import chat.dim.format.Base64;
import chat.dim.format.Hex;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.MetaUtils;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.Facebook;

public class CryptoTest {

    private static final Facebook facebook = SharedFacebook.getInstance();

    @Test
    public void testHash() {
        Log.info("Crypto test");

        String string = "moky";
        byte[] data = UTF8.encode(string);

        byte[] hash;
        String res;
        String exp;

        // sha256（moky）= cb98b739dd699aa44bb6ebba128d20f2d1e10bb3b4aa5ff4e79295b47e9ed76d
        hash = SHA256.digest(data);
        res = Hex.encode(hash);
        exp = "cb98b739dd699aa44bb6ebba128d20f2d1e10bb3b4aa5ff4e79295b47e9ed76d";
        Log.info("sha256(" + string + ") = " + res);
        Assert.assertEquals(exp, res);

        // ripemd160(moky) = 44bd174123aee452c6ec23a6ab7153fa30fa3b91
        hash = RIPEMD160.digest(data);
        res = Hex.encode(hash);
        exp = "44bd174123aee452c6ec23a6ab7153fa30fa3b91";
        Log.info("ripemd160(" + string + ") = " + res);
        Assert.assertEquals(exp, res);
    }

    private void testKeccak(String string, String exp) {
        byte[] data = UTF8.encode(string);
        byte[] hash = Keccak256.digest(data);
        String res = Hex.encode(hash);
        Log.info("Keccak256 ( " + string + " ):\n\t" + res);
        Assert.assertEquals(exp, res);
    }

    @Test
    public void testKeccak() {
        // https://keccak-256.cloxy.net

        Log.info("test Keccak");

        testKeccak("moky", "96b07f3103d45cc7df2dd6e597922a17f48c86257dffe790d442bbd1ff46514d");
        testKeccak("hello", "1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8");
        testKeccak("abc", "4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45");

        testKeccak("0450863ad64a87ae8a2fe83c1af1a8403cb53f53e486d8511dad8a04887e5b23522cd470243453a299fa9e77237716103abc11a1df38855ed6f2ee187e9c582ba6",
                "fc12ad814631ba689f7abe671016f75c54c607f082ae6b0881fac0abeda21781");

//        testKeccak("044a18c2c740f49a77b289e9270c39948b9410a1b0c981d9af068c06239363d72682fdb022fef67f4f8a69582f983ab394ab5f06854c25f33d8ef1352fe7fe504d",
//                "24602722816b6cad0e143ce9fabf31f6026ec622");
    }

    @Test
    public void testEncode() {
        String string = "moky";
        byte[] data = UTF8.encode(string);

        String res;
        String exp;

        // base58(moky) = 3oF5MJ
        res = Base58.encode(data);
        exp = "3oF5MJ";
        Log.info("base58(" + string + ") = " + res);
        Assert.assertEquals(exp, res);

        // base64(moky) = bW9reQ==
        res =Base64.encode(data);
        exp = "bW9reQ==";
        Log.info("base64(" + string + ") = " + res);
        Assert.assertEquals(exp, res);
    }

    @Test
    public void testMeta() {
        String username = "moky";
        PrivateKey sk = PrivateKey.generate("RSA");
        Meta meta = Meta.generate(MetaType.MKM, sk, username);
        Log.info("meta: " + JSON.encode(meta));
        Log.info("SK: " + JSON.encode(sk));
    }

    private void checkX(String metaJson, String skJson) {
        Object metaDict = JSON.decode(metaJson);
        Meta meta = Meta.parse(metaDict);
        ID identifier = ID.generate(meta, EntityType.USER.value, null);
        Log.info("meta: " + meta);
        Log.info("ID: " + identifier);

        Object skDict = JSON.decode(skJson);
        PrivateKey sk = PrivateKey.parse(skDict);
        Log.info("private key: " + sk);
        Assert.assertTrue(MetaUtils.matches(sk.getPublicKey(), meta));

        Map<String, Object> extra = new HashMap<>();

        String name = "moky";
        byte[] data = UTF8.encode(name);
        byte[] CT = ((EncryptKey) meta.getPublicKey()).encrypt(data, extra);
        byte[] PT = ((DecryptKey) sk).decrypt(CT, extra);
        String hex = Hex.encode(CT);
        String res = UTF8.decode(PT);
        Log.info("encryption: " + name + ", -> " + hex + " -> " + res);
    }

    @Test
    public void testPythonMeta() {
        Log.info("checking data from Python ...");
        String s1 = "{\"type\": 1, \"seed\": \"moky\", \"key\": {\"algorithm\": \"RSA\", \"data\": \"-----BEGIN PUBLIC KEY-----\\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDbTf58ScygpB7tbOSN5/9dZIEB\\ncbKjnwUxW8cWdEo765gGXUsNUQUgThFC4csLsFTvqVhGxn3+WfDIwrvzF/2HWJu8\\nUvo3LkG3ZGP8US9y2Mvp9VaCWz8ZwyMe4fcWus8dRs8fdTatLeoZVGQDv9SdE2Vx\\nuPvJw3gHyR9dOKq37QIDAQAB\\n-----END PUBLIC KEY-----\"}, \"fingerprint\": \"MTHQiV/6sCAtOM6CJ2clJgKHu4Lyw34rzoWLsARPL61Z4ivz/q2y9dXIN49A0B+RT6z7+vqI6NGseTLZoc1wT7EHN5qDIYWMdJjOd7YGv5K/AFQCMkZxcpDb51ryWC22/n2bi1MvkH+b3lhQjwvtwqq05K1nDixUTFqAEcNQDOg=\"}\n";
        String s2 = "{\"algorithm\": \"RSA\", \"data\": \"-----BEGIN RSA PRIVATE KEY-----\\nMIICXAIBAAKBgQDbTf58ScygpB7tbOSN5/9dZIEBcbKjnwUxW8cWdEo765gGXUsN\\nUQUgThFC4csLsFTvqVhGxn3+WfDIwrvzF/2HWJu8Uvo3LkG3ZGP8US9y2Mvp9VaC\\nWz8ZwyMe4fcWus8dRs8fdTatLeoZVGQDv9SdE2VxuPvJw3gHyR9dOKq37QIDAQAB\\nAoGADLCdbHM3xkbg7EOsEQMO7YhKh7scx2eE/SdupH+7qO53yEx/MojQ517lFE3s\\n+iLss0aFD2lecoihTHiqOAWYG7CfMRg7OaG5Kx7MCZdo/fm28DtiymjpB6nR1SFu\\nmroBWN2rDWHiYBSLeZM8Efh8ONhSue6zMwCIzatBfhrp5JkCQQDfuevIWO9WYRQi\\ndpvDdHzf7uK7ypAs+h90NdJ5P2ihYW7EkSioTu1tskI+d71p/pzTWMd6u/3wWKGV\\n95Yz2wQ5AkEA+vDJToLYObUStmGp+FVsKU4JezHfd831pMcx09nG3d3AIHcJfKx7\\nJzY6k1KVUE3nPHMwhIxEV1AOsOiImU7ZVQJBAKL7b5AZceoMeL2OiHTAJMSB470I\\nmTWa1VU0bGsVzWRbdXVPhj3umbrjNK0LT/qqmJbCwzdfQmRYPQbiQhLux8kCQG4x\\nzISwipkUvcnfK0+E24Fr5lf196bZh7Q7UNMx/9Uv6o2XGFBqQY5fjutgyXbBLvjp\\nsHWUTvJ0km73Pfzslh0CQGLWIE3osF9bLOy7xhLzbVFf3y0yFI/0/pyYvBMZ3yCy\\nw65R9OCcY2PFM2SGEw+nQtopShcpKT0xG30P11TO9oA=\\n-----END RSA PRIVATE KEY-----\"}\n";
        checkX(s1, s2);
    }

    @Test
    public void testOCMeta() {
        Log.info("checking data from Obj-C ...");
        String s1 = "{\"fingerprint\":\"HLpTHlWIr\\/Hzd7l9\\/+8iAYgeOQI2gezR4TLs\\/WYR0sJZYKnVJeHeTijbwtze0t7Ak\\/K7U8TVnnlubRXE7N\\/Dio0\\/1PZmhoW95j40zVOjlxc13n54S4ZKVH7laZUmGtNLEkKsj+4Vr\\/RybJJhGQMwHO2h3q0ueIQj6nrI6fWCX9I=\",\"key\":{\"algorithm\":\"RSA\",\"data\":\"-----BEGIN PUBLIC KEY-----\\nMIGJAoGBANHKUezJ7gH1RTnYhnRZrMrONd3\\/RKV+UthgU4uwVsA7jJz\\/JwJWhsLY\\nX1BsSJRcQRnpWYSUxkzjU34FhUW28oFPvTrtzFM41AhLhE8MYP8vy9\\/\\/TK0OAFwY\\nwY1pqEJJKdbbOVCHLI1Ddu1CbHmLdQ2NF+10nszrJGxJLDnNadZnAgMBAAE=\\n-----END PUBLIC KEY-----\\n\",\"digest\":\"SHA256\",\"mode\":\"ECB\",\"padding\":\"PKCS1\"},\"seed\":\"moky\",\"type\":1}";
        String s2 = "{\"algorithm\":\"RSA\",\"data\":\"-----BEGIN RSA PRIVATE KEY-----\\nMIICXAIBAAKBgQDRylHsye4B9UU52IZ0WazKzjXd\\/0SlflLYYFOLsFbAO4yc\\/ycC\\nVobC2F9QbEiUXEEZ6VmElMZM41N+BYVFtvKBT7067cxTONQIS4RPDGD\\/L8vf\\/0yt\\nDgBcGMGNaahCSSnW2zlQhyyNQ3btQmx5i3UNjRftdJ7M6yRsSSw5zWnWZwIDAQAB\\nAoGAKeT00kwK9yIjVmdqhk6oJoHimPgSndfptGMcG\\/+1e0MJFAsSH7HmzH9IHXfa\\nUKJRr9p9MXBCX3VgJYD1udPMfnCxCnL9CLnqxjPWJ+SISumV2g8PYVEPCVnN+zBp\\njBLpoeQS43c4heyF3DM41x6QrSGXtofUJ1W4U0VejnvlosECQQDvqp\\/6rkT4mjqj\\nMAGHWnIr2cbnt2UqQH85viSx3pLyPXn5FnDI1EiEU\\/Pi+XuxoTCtxWd+gx9aWRpY\\n+mXcK\\/YnAkEA4BZ0ukPDr8e5KmQN7x5x\\/CfHhqPRGVk4VH9z+icJ0\\/DvH9+7Nj30\\n5i8T6kAyGWdYoxkhQydmwi6Fpx6SxGWFwQJAQxkV6OzZSnCDciSCiQ59YGF8Gmtx\\n2z5rYBMn2tRhd4hWmbH6qX8lPkbyxNzsEHL8Weoma3jyUi0X\\/0k7M0TriQJBALv6\\nGnEl50HNiMbGp+mu4G9l7zpCsWVSMq6vO9rcZKIlunJCfAlEb+uoEkyvDVfCGdi3\\ne++ZXdoGrJdETlnx0AECQDQEW7kuBhHQ4cZ9v+qY7PfM87qM7EdsCm1QTNJ48n9I\\nJWeoVaoTQkKb+vB\\/JOjuNx9FHmqySKkNMkTqPNlFCFI=\\n-----END RSA PRIVATE KEY-----\\n\",\"digest\":\"SHA256\",\"mode\":\"ECB\",\"padding\":\"PKCS1\"}";
        checkX(s1, s2);
    }

}
