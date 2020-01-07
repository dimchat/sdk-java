
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import chat.dim.AddressNameService;
import chat.dim.ID;
import chat.dim.Immortals;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.PrivateKey;

public class Facebook extends chat.dim.Facebook {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();

        // ANS
        ans = new AddressNameService() {
            @Override
            public ID identifier(String name) {
                return null;
            }

            @Override
            public boolean save(String name, ID identifier) {
                return false;
            }
        };
        setANS(ans);
    }

    private final AddressNameService ans;
    private Immortals immortals = new Immortals();

    private List<User> users = null;

    private Map<ID, Date> metaQueryTime = new HashMap<>();
    private Map<ID, Date> profileQueryTime = new HashMap<>();

    //-------- Local Users

    @Override
    public List<User> getLocalUsers() {
        return users;
    }

    @Override
    public User getCurrentUser() {
        return null;
    }

    public void setCurrentUser(User user) {
        users = null;
    }

    public boolean addUser(ID user) {
        users = null;
        return false;
    }

    public boolean removeUser(ID user) {
        users = null;
        return false;
    }

    //-------- Contacts

    public boolean addContact(ID contact, ID user) {
        return false;
    }

    public boolean removeContact(ID contact, ID user) {
        return false;
    }

    //-------- Private Key

    @Override
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        if (!verify(privateKey, identifier)) {
            // private key not match meta.key
            return false;
        }
        return false;
    }

    @Override
    protected PrivateKey loadPrivateKey(ID user) {
        // FIXME: which key?
        PrivateKey key = null;
        if (key == null) {
            // try immortals
            key = (PrivateKey) immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    //-------- Meta

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!verify(meta, entity)) {
            // meta not match ID
            return false;
        }
        return false;
    }

    @Override
    protected Meta loadMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has not meta
            return null;
        }
        // try from database
        Meta meta = null;
        if (meta != null) {
            return meta;
        }
        // try from immortals
        if (identifier.getType().isPerson()) {
            meta = immortals.getMeta(identifier);
            if (meta != null) {
                return meta;
            }
        }

        // check for duplicated querying
        Date now = new Date();
        Date lastTime = metaQueryTime.get(identifier);
        if (lastTime == null || (now.getTime() - lastTime.getTime()) > 30000) {
            metaQueryTime.put(identifier, now);
            // TODO: query from DIM network
        }

        return null;
    }

    //-------- Profile

    @Override
    public boolean saveProfile(Profile profile) {
        if (!verify(profile)) {
            // profile's signature not match
            return false;
        }
        return false;
    }

    @Override
    protected Profile loadProfile(ID identifier) {
        // try from database
        Profile profile = null;
        if (profile != null) {
            // is empty?
            Set<String> names = profile.propertyNames();
            if (names != null && names.size() > 0) {
                return profile;
            }
        }
        // try from immortals
        if (identifier.getType().isPerson()) {
            Profile tai = immortals.getProfile(identifier);
            if (tai != null) {
                return tai;
            }
        }

        // check for duplicated querying
        Date now = new Date();
        Date lastTime = profileQueryTime.get(identifier);
        if (lastTime == null || (now.getTime() - lastTime.getTime()) > 30000) {
            profileQueryTime.put(identifier, now);
            // TODO: query from DIM network
        }

        return profile;
    }

    //-------- Relationship

    @Override
    public boolean saveContacts(List<ID> contacts, ID user) {
        return false;
    }

    @Override
    protected List<ID> loadContacts(ID user) {
        List<ID> contacts = null;
        if (contacts == null || contacts.size() == 0) {
            // try immortals
            contacts = immortals.getContacts(user);
        }
        return contacts;
    }

    public boolean addMember(ID member, ID group) {
        return false;
    }

    public boolean removeMember(ID member, ID group) {
        return false;
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        return false;
    }

    @Override
    protected List<ID> loadMembers(ID group) {
        return null;
    }

    //--------

    public String getUsername(Object string) {
        return getUsername(getID(string));
    }

    public String getUsername(ID identifier) {
        String username = identifier.name;
        String nickname = getNickname(identifier);
        if (nickname != null && nickname.length() > 0) {
            if (identifier.getType().isUser()) {
                if (username != null && username.length() > 0) {
                    return nickname + " (" + username + ")";
                }
            }
            return nickname;
        } else if (username != null && username.length() > 0) {
            return username;
        }
        // ID only contains address: BTC, ETH, ...
        return identifier.address.toString();
    }

    public String getNickname(ID identifier) {
        assert identifier.getType().isUser();
        Profile profile = getProfile(identifier);
        return profile == null ? null : profile.getName();
    }

    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format(Locale.CHINA, "%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        // get from database
        ID founder = null;
        if (founder != null) {
            return founder;
        }
        return super.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        // get from database
        ID owner = null;
        if (owner != null) {
            return owner;
        }
        return super.getOwner(group);
    }
}
