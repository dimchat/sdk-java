
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.compat.CompatibleMetaFactory;
import chat.dim.core.Archivist;
import chat.dim.core.Barrack;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.mkm.MetaUtils;
import chat.dim.plugins.ExtensionLoader;
import chat.dim.plugins.NativePluginLoader;
import chat.dim.plugins.PluginLoader;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public class SharedFacebook extends Facebook {
    private static final SharedFacebook ourInstance = new SharedFacebook();
    public static SharedFacebook getInstance() { return ourInstance; }
    private SharedFacebook() {
        super();
    }

    @Override
    public Barrack getBarrack() {
        // TODO:
        return null;
    }

    @Override
    public Archivist getArchivist() {
        return null;
    }

    // memory caches
    private final Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private final Map<ID, Meta>       metaMap  = new HashMap<>();

    protected void cachePrivateKey(PrivateKey key, ID identifier) {
        if (key == null) {
            privateKeyMap.remove(identifier);
        } else {
            privateKeyMap.put(identifier, key);
        }
    }

    protected void cacheMeta(Meta meta, ID identifier) {
        assert MetaUtils.matches(identifier, meta) : "meta not match ID: " + identifier + ", " + meta;
        metaMap.put(identifier, meta);
    }

    //-------- Entity DataSource

    @Override
    public Meta getMeta(ID entity) {
        return metaMap.get(entity);
    }

    @Override
    public List<Document> getDocuments(ID entity) {
        // TODO:
        return null;
    }

    //------- User DataSource

    @Override
    public List<ID> getContacts(ID user) {
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

        Meta.Factory mkm = new CompatibleMetaFactory(MetaType.MKM);
        Meta.Factory btc = new CompatibleMetaFactory(MetaType.BTC);
        Meta.Factory eth = new CompatibleMetaFactory(MetaType.ETH);

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

    static {
        new ExtensionLoader().run();

        new PluginLoader().run();
        new NativePluginLoader().run();

        registerCompatibleMetaFactories();
    }
}
