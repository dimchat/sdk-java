
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.GroupDataSource;
import chat.dim.Immortals;
import chat.dim.User;
import chat.dim.UserDataSource;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class Facebook implements UserDataSource, GroupDataSource {
    private static Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    // immortals
    private Immortals immortals = new Immortals();

    // memory caches
    private Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, Meta>       metaMap  = new HashMap<>();
    private Map<ID, User>       userMap  = new HashMap<>();

    public boolean cache(PrivateKey key, ID identifier) {
        if (key == null) {
            privateKeyMap.remove(identifier);
            return false;
        }
        privateKeyMap.put(identifier, key);
        return true;
    }

    public boolean cache(Meta meta, ID identifier) {
        assert meta.matches(identifier) : "meta not match ID: " + identifier + ", " + meta;
        metaMap.put(identifier, meta);
        return true;
    }

    public boolean cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.identifier, user);
        return true;
    }

    public User getUser(ID identifier) {
        User user = userMap.get(identifier);
        if (user == null) {
            user = immortals.getUser(identifier);
            if (user == null) {
                user = new User(identifier);
                cache(user);
            }
        }
        return user;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID entity) {
        Meta meta = metaMap.get(entity);
        if (meta == null) {
            meta = immortals.getMeta(entity);
        }
        return meta;
    }

    @Override
    public Document getDocument(ID entity, String type) {
        return immortals.getDocument(entity, type);
    }

    //------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return immortals.getContacts(user);
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // NOTICE: return nothing to use meta.key
        return null;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        SignKey key = privateKeyMap.get(user);
        if (key == null) {
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        PrivateKey key = privateKeyMap.get(user);
        if (key == null) {
            return immortals.getPrivateKeysForDecryption(user);
        } else if (key instanceof DecryptKey) {
            List<DecryptKey> keys = new ArrayList<>();
            keys.add((DecryptKey) key);
            return keys;
        }
        return null;
    }

    @Override
    public SignKey getPrivateKeyForVisaSignature(ID user) {
        return getPrivateKeyForSignature(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        return null;
    }
}
