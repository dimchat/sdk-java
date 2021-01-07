
package chat.dim;

import java.util.Map;

public class KeyStore extends KeyCache {

    private static final KeyStore ourInstance = new KeyStore();
    public static KeyStore getInstance() { return ourInstance; }
    private KeyStore() {
        super();
    }

    @Override
    public boolean saveKeys(Map keyMap) {
        // TODO: saving cipher key table into local storage
        return false;
    }

    @Override
    public Map loadKeys() {
        // TODO: load cipher key table from local storage
        return null;
    }
}
