
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;

public class Facebook implements User.DataSource, Group.DataSource {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();

        chat.dim.Plugins.registerPlugins();
        chat.dim.CryptoPlugins.registerCryptoPlugins();
    }

    // memory caches
    private final Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private final Map<ID, Meta>       metaMap  = new HashMap<>();
    private final Map<ID, User>       userMap  = new HashMap<>();

    public boolean cache(PrivateKey key, ID identifier) {
        if (key == null) {
            privateKeyMap.remove(identifier);
            return false;
        }
        privateKeyMap.put(identifier, key);
        return true;
    }

    public boolean cache(Meta meta, ID identifier) {
        assert Meta.matches(identifier, meta) : "meta not match ID: " + identifier + ", " + meta;
        metaMap.put(identifier, meta);
        return true;
    }

    public boolean cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.getIdentifier(), user);
        return true;
    }

    public User getUser(ID identifier) {
        User user = userMap.get(identifier);
        if (user == null) {
            user = new BaseUser(identifier);
            cache(user);
        }
        return user;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID entity) {
        return metaMap.get(entity);
    }

    @Override
    public Document getDocument(ID entity, String type) {
        return null;
    }

    //------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return null;
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // 1. get key from visa
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa) {
            EncryptKey key = ((Visa) doc).getPublicKey();
            if (key != null) {
                return key;
            }
        }
        // 2. get key from meta
        Meta meta = getMeta(user);
        if (meta != null) {
            Object key = meta.getPublicKey();
            if (key instanceof EncryptKey) {
                return (EncryptKey) key;
            }
        }
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        List<VerifyKey> keys = new ArrayList<>();
        // 1. get key from visa
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa) {
            Object key = ((Visa) doc).getPublicKey();
            if (key instanceof VerifyKey) {
                // the sender may use communication key to sign message.data,
                // so try to verify it with visa.key here
                keys.add((VerifyKey) key);
            }
        }
        // 2. get key from meta
        Meta meta = getMeta(user);
        if (meta != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key
            keys.add(meta.getPublicKey());
        }
        return keys;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        return privateKeyMap.get(user);
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        PrivateKey key = privateKeyMap.get(user);
        if (key instanceof DecryptKey) {
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

    @Override
    public List<ID> getAssistants(ID group) {
        Document doc = getDocument(group, Document.BULLETIN);
        if (doc instanceof Bulletin) {
            return ((Bulletin) doc).getAssistants();
        }
        // TODO: get group bots from SP configuration
        return null;
    }
}
