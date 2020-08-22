
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Group;
import chat.dim.ID;
import chat.dim.Immortals;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;

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
    private Map<ID, Profile>    profileMap    = new HashMap<>();

    // "/sdcard/chat.dim.sechat/.mkm/"
    public String directory = "/tmp/.mkm/";

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        return false;
    }

    @Override
    public boolean saveProfile(Profile profile) {
        return false;
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        return false;
    }

    @Override
    public List<User> getLocalUsers() {
        return null;
    }

    //---- Private Key

    protected boolean cachePrivateKey(PrivateKey key, ID identifier) {
        privateKeyMap.put(identifier, key);
        return true;
    }

    //---- Profile

    protected boolean cacheProfile(Profile profile) {
        ID identifier = ID.getInstance(profile.getIdentifier());
        profileMap.put(identifier, profile);
        return true;
    }

    //-------- SocialNetworkDataSource

    @Override
    public User getUser(ID identifier) {
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        user = new User(identifier);
        cache(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        Group group = super.getGroup(identifier);
        if (group == null) {
            group = new Group(identifier);
            cache(group);
        }
        return group;
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
    public Profile getProfile(ID entity) {
        return profileMap.get(entity);
    }

    //---- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return immortals.getContacts(user);
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
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // NOTICE: return nothing to use meta.key
        return null;
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
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // NOTICE: return nothing to use profile.key or meta.key
        return null;
    }

    //---- GroupDataSource

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
        return null;
    }
}
