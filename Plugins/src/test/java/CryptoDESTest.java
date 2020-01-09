
import org.junit.Test;

import java.util.Map;

import chat.dim.crypto.SymmetricKey;

public class CryptoDESTest {

    @Test
    public void testDES() {

    }
}

final class DESKey extends SymmetricKey {

    public DESKey(Map<String, Object> dictionary) {
        super(dictionary);
        // TODO: check algorithm parameters
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
    public byte[] decrypt(byte[] ciphertext) {
        return new byte[0];
    }

    static {
        register(SymmetricKey.DES, DESKey.class);
    }
}
