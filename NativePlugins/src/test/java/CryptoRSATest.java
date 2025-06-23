
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.AsymmetricAlgorithms;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.format.Hex;
import chat.dim.format.UTF8;
import chat.dim.Facebook;

public class CryptoRSATest {

    private static final Facebook facebook = SharedFacebook.getInstance();

    @Test
    public void testRSA() {
        PrivateKey sk = PrivateKey.generate(AsymmetricAlgorithms.RSA);
        Log.info("RSA private key: " + sk);

        PublicKey pk = sk.getPublicKey();
        Log.info("RSA public key: " + pk);

        Map<String, Object> extra = new HashMap<>();

        String text = "moky";
        byte[] plaintext = UTF8.encode(text);
        byte[] ciphertext = ((EncryptKey) pk).encrypt(plaintext, extra);
        Log.info("RSA encrypt(\"" + text + "\") = " + Hex.encode(ciphertext));

        byte[] data = ((DecryptKey) sk).decrypt(ciphertext, extra);
        String decrypt = new String(data);
        Log.info("decrypt to " + decrypt);

        Assert.assertEquals(text, decrypt);

        byte[] signature = sk.sign(plaintext);
        Log.info("signature(\"" + text + "\") = " + Hex.encode(signature));

        boolean ok = pk.verify(plaintext, signature);
        Assert.assertTrue(ok);
    }

    @Test
    public void testPublicKey() {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "RSA");
        dictionary.put("data", "-----BEGIN PUBLIC KEY-----\n" +
                "MIGJAoGBALB+vbUK48UU9rjlgnohQowME+3JtTb2hLPqtatVOW364/EKFq0/PSdnZVE9V2Zq+pbX7dj3nCS4pWnYf40ELH8wuDm0Tc4jQ70v4LgAcdy3JGTnWUGiCsY+0Z8kNzRkm3FJid592FL7ryzfvIzB9bjg8U2JqlyCVAyUYEnKv4lDAgMBAAE=\n" +
                "-----END PUBLIC KEY-----");

        PublicKey key = PublicKey.parse(dictionary);
        Log.info("public key: " + key);
    }

    @Test
    public void testPrivateKey() {
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("algorithm", "RSA");
        dictionary.put("data", "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIICXQIBAAKBgQCwfr21CuPFFPa45YJ6IUKMDBPtybU29oSz6rWrVTlt+uPxChatPz0nZ2VRPVdmavqW1+3Y95wkuKVp2H+NBCx/MLg5tE3OI0O9L+C4AHHctyRk51lBogrGPtGfJDc0ZJtxSYnefdhS+68s37yMwfW44PFNiapcglQMlGBJyr+JQwIDAQABAoGAVc0HhJ/KouDSIIjSqXTJ2TN17L+GbTXixWRw9N31kVXKwj9ZTtfTbviA9MGRX6TaNcK7SiL1sZRiNdaeC3vf9RaUe3lV3aR/YhxuZ5bTQNHPYqJnbbwsQkp4IOwSWqOMCfsQtP8O+2DPjC8Jx7PPtOYZ0sC5esMyDUj/EDv+HUECQQDXsPlTb8BAlwWhmiAUF8ieVENR0+0EWWU5HV+dp6Mz5gf47hCO9yzZ76GyBM71IEQFdtyZRiXlV9CBOLvdlbqLAkEA0XqONVaW+nNTNtlhJVB4qAeqpj/foJoGbZhjGorBpJ5KPfpD5BzQgsoT6ocv4vOIzVjAPdk1lE0ACzaFpEgbKQJBAKDLjUO3ZrKAI7GSreFszaHDHaCuBd8dKcoHbNWiOJejIERibbO27xfVfkyxKvwwvqT4NIKLegrciVMcUWliivsCQQCiA1Z/XEQS2iUO89tVn8JhuuQ6Boav0NCN7OEhQxX3etFS0/+0KrD9psr2ha38qnwwzaaJbzgoRdF12qpL39TZAkBPv2lXFNsn0/Jq3cUemof+5sm53KvtuLqxmZfZMAuTSIbB+8i05JUVIc+mcYqTqGp4FDfz6snzt7sMBQdx6BZY\n" +
                "-----END RSA PRIVATE KEY-----");

        PrivateKey sk = PrivateKey.parse(dictionary);
        Log.info("private key: " + sk);
    }
}
