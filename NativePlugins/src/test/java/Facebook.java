
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Archivist;
import chat.dim.Barrack;
import chat.dim.compat.CompatibleMetaFactory;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class Facebook extends Barrack implements User.DataSource, Group.DataSource {
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
    protected void cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        super.cache(user);
    }

    @Override
    protected void cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        super.cache(group);
    }

    @Override
    public Archivist getArchivist() {
        // TODO:
        return null;
    }

    //-------- Entity DataSource

    @Override
    public Meta getMeta(ID entity) {
        return metaMap.get(entity);
    }

    @Override
    public List<Document> getDocuments(ID entity) {
        return null;
    }

    //------- User DataSource

    @Override
    public List<ID> getContacts(ID user) {
        // TODO:
        return null;
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // TODO:
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // TODO:
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

    //------- Group DataSource

    @Override
    public ID getFounder(ID group) {
        // TODO:
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        // TODO:
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        // TODO:
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        // TODO:
        return null;
    }

    /**
     *  Meta factories
     */
    static void registerCompatibleMetaFactories() {

        Meta.Factory mkm = new CompatibleMetaFactory(Meta.MKM);
        Meta.Factory btc = new CompatibleMetaFactory(Meta.BTC);
        Meta.Factory eth = new CompatibleMetaFactory(Meta.ETH);

        Meta.setFactory("1", mkm);
        Meta.setFactory("2", btc);
        Meta.setFactory("4", eth);

        Meta.setFactory("mkm", mkm);
        Meta.setFactory("btc", btc);
        Meta.setFactory("eth", eth);

        Meta.setFactory("MKM", mkm);
        Meta.setFactory("BTC", btc);
        Meta.setFactory("ETH", eth);
    }

    static void registerPlugins() {
        chat.dim.Plugins.registerPlugins();
        chat.dim.NativePlugins.registerNativePlugins();

        registerCompatibleMetaFactories();
    }

    static {
        registerPlugins();
    }
}
