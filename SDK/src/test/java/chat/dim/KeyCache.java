
package chat.dim;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.PlainKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.ID;

/**
 *  Symmetric Keys Cache
 *  ~~~~~~~~~~~~~~~~~~~~
 *  Manage keys for conversations
 */
public abstract class KeyCache implements CipherKeyDelegate {

    // memory caches
    private Map<ID, Map<ID, SymmetricKey>> keyMap = new HashMap<>();
    private boolean isDirty = false;

    protected KeyCache() {
        super();
    }

    /**
     *  Trigger for loading cipher key table
     *
     * @return true on success
     */
    public boolean reload() {
        Map dictionary = loadKeys();
        if (dictionary == null) {
            return false;
        }
        return updateKeys(dictionary);
    }

    /**
     *  Trigger for saving cipher key table
     */
    public void flush() {
        if (!isDirty) {
            // nothing changed
            return;
        }
        if (saveKeys(keyMap)) {
            // keys saved
            isDirty = false;
        }
    }

    /**
     *  Callback for saving cipher key table into local storage
     *  (Override it to access database)
     *
     * @param keyMap - all cipher keys(with direction) from memory cache
     * @return YES on success
     */
    public abstract boolean saveKeys(Map keyMap);

    /**
     *  Load cipher key table from local storage
     *  (Override it to access database)
     *
     * @return keys map
     */
    public abstract Map loadKeys();

    /**
     *  Update cipher key table into memory cache
     *
     * @param keyMap - cipher keys(with direction) from local storage
     * @return NO on nothing changed
     */
    @SuppressWarnings("unchecked")
    public boolean updateKeys(Map keyMap) {
        if (keyMap == null || keyMap.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)keyMap;
        for (Map.Entry<String, Map<String, Object>> entry1 : map.entrySet()) {
            ID from = ID.parse(entry1.getKey());
            Map<String, Object> table = entry1.getValue();
            for (Map.Entry<String, Object> entity2 : table.entrySet()) {
                ID to = ID.parse(entity2.getKey());
                SymmetricKey newKey = SymmetricKey.parse((Map<String, Object>) entity2.getValue());
                assert newKey != null : "key error(" + from + " -> " + to + "): " + entity2.getValue();
                // check whether exists an old key
                SymmetricKey oldKey = getKey(from, to);
                if (oldKey != newKey) {
                    changed = true;
                }
                // cache key with direction
                setKey(from, to, newKey);
            }
        }
        return changed;
    }

    private SymmetricKey getKey(ID from, ID to) {
        Map<ID, SymmetricKey> keyTable = keyMap.get(from);
        return keyTable == null ? null : keyTable.get(to);
    }

    private void setKey(ID from, ID to, SymmetricKey key) {
        assert key != null : "cipher key cannot be empty";
        Map<ID, SymmetricKey> keyTable = keyMap.get(from);
        if (keyTable == null) {
            keyTable = new HashMap<>();
            keyMap.put(from, keyTable);
        } else {
            SymmetricKey old = keyTable.get(to);
            if (old != null) {
                // check whether same key exists
                boolean equals = true;
                String k;
                Object v1, v2;
                for (Map.Entry<String, Object> entity : key.entrySet()) {
                    k = entity.getKey();
                    v1 = entity.getValue();
                    v2 = old.get(k);
                    if (v1 == null) {
                        if (v2 == null) {
                            continue;
                        }
                    } else if (v1.equals(v2)) {
                        continue;
                    }
                    equals = false;
                    break;
                }
                if (equals) {
                    // no need to update
                    return;
                }
            }
        }
        //Map<ID, SymmetricKey> keyTable = keyMap.computeIfAbsent(from, k -> new HashMap<>());
        keyTable.put(to, key);
    }

    //-------- CipherKeyDelegate

    @Override
    public SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate) {
        if (receiver.isBroadcast()) {
            return PlainKey.getInstance();
        }
        // get key from cache
        SymmetricKey key = getKey(sender, receiver);
        if (key == null && generate) {
            // generate new key and cache it
            key = SymmetricKey.generate(SymmetricKey.AES);
            cacheCipherKey(sender, receiver, key);
        }

        // TODO: override to check whether key expired for sending message
        return key;
    }

    @Override
    public void cacheCipherKey(ID sender, ID receiver, SymmetricKey key) {
        if (receiver.isBroadcast()) {
            // broadcast message has no key
            return;
        }
        setKey(sender, receiver, key);
        isDirty = true;
    }
}
