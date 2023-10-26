
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Barrack;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class Facebook extends Barrack {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    // memory caches
    private final Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private final Map<ID, Meta>       metaMap  = new HashMap<>();

    protected void cache(PrivateKey key, ID identifier) {
        if (key == null) {
            privateKeyMap.remove(identifier);
        } else {
            privateKeyMap.put(identifier, key);
        }
    }

    protected void cache(Meta meta, ID identifier) {
        assert meta.matchIdentifier(identifier) : "meta not match ID: " + identifier + ", " + meta;
        metaMap.put(identifier, meta);
    }

    @Override
    protected User createUser(ID identifier) {
        assert identifier.isUser() : "user ID error: " + identifier;
        // check visa key
        if (!identifier.isBroadcast()) {
            if (getPublicKeyForEncryption(identifier) == null) {
                assert false : "visa.key not found: " + identifier;
                return null;
            }
            // NOTICE: if visa.key exists, then visa & meta must exist too.
        }
        // TODO: check user type

        // general user, or 'anyone@anywhere'
        return new BaseUser(identifier);
    }

    @Override
    protected Group createGroup(ID identifier) {
        assert identifier.isGroup() : "group ID error: " + identifier;
        // check members
        if (!identifier.isBroadcast()) {
            List<ID> members = getMembers(identifier);
            if (members == null || members.isEmpty()) {
                assert false : "group members not found: " + identifier;
                return null;
            }
            // NOTICE: if members exist, then owner (founder) must exist,
            //         and bulletin & meta must exist too.
        }
        // TODO: check group type

        // general group, or 'everyone@everywhere'
        return new BaseGroup(identifier);
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID entity) {
        return metaMap.get(entity);
    }

    @Override
    public List<Document> getDocuments(ID entity) {
        return null;
    }

    //------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return null;
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

    static {
        chat.dim.Plugins.registerPlugins();
        chat.dim.CryptoPlugins.registerCryptoPlugins();
    }
}
