
import org.junit.Test;

import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.DecryptKey;

public class CryptoECCTest {

    @Test
    public void testECC() {
    }
}

final class ECCPrivateKey extends PrivateKey implements DecryptKey {

    public ECCPrivateKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public PublicKey getPublicKey() {
        return null;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return new byte[0];
    }

    @Override
    public byte[] sign(byte[] data) {
        return new byte[0];
    }

    static {
        register(PrivateKey.ECC, ECCPrivateKey.class);
    }
}

final class ECCPublicKey extends PublicKey implements EncryptKey {

    public ECCPublicKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return new byte[0];
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        return false;
    }

    static {
        register(PublicKey.ECC, ECCPublicKey.class);
    }
}
