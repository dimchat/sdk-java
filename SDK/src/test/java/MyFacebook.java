
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Immortals;
import chat.dim.User;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class MyFacebook extends Facebook {
    private static MyFacebook ourInstance = new MyFacebook();
    public static MyFacebook getInstance() { return ourInstance; }
    private MyFacebook() {
        super();
    }

    // immortals
    private Immortals immortals = new Immortals();

    // memory caches
    private Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, Meta>       metaMap       = new HashMap<>();
    private Map<ID, Document>   docMap        = new HashMap<>();

    // "/sdcard/chat.dim.sechat/.mkm/"
    public String directory = "/tmp/.mkm/";

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        return false;
    }

    @Override
    public boolean saveDocument(Document doc) {
        return false;
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        return false;
    }

    @Override
    public List<User> getLocalUsers() {
        List<User> users = new ArrayList<>();
        users.add(immortals.getUser(ID.parse(Immortals.MOKI)));
        users.add(immortals.getUser(ID.parse(Immortals.HULK)));
        return users;
    }

    //---- Private Key

    protected boolean cachePrivateKey(PrivateKey key, ID identifier) {
        privateKeyMap.put(identifier, key);
        return true;
    }

    //---- Entity Document

    protected boolean cacheDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        docMap.put(identifier, doc);
        return true;
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = metaMap.get(identifier);
        if (meta == null) {
            meta = immortals.getMeta(identifier);
        }
        return meta;
    }

    @Override
    public Document getDocument(ID entity, String type) {
        return docMap.get(entity);
    }

    //---- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return immortals.getContacts(user);
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
    public SignKey getPrivateKeyForSignature(ID user) {
        SignKey key = privateKeyMap.get(user);
        if (key == null) {
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public SignKey getPrivateKeyForVisaSignature(ID user) {
        return getPrivateKeyForSignature(user);
    }

    //---- GroupDataSource

    @Override
    public List<ID> getMembers(ID group) {
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        return null;
    }
}
