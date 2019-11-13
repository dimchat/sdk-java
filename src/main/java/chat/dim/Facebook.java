/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim;

import java.util.*;

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.Address;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.Group;
import chat.dim.mkm.LocalUser;
import chat.dim.network.Station;

public abstract class Facebook extends Barrack {

    public AddressNameService ans = null;

    // memory caches
    private Map<ID, Profile>    profileMap    = new HashMap<>();
    private Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, List<ID>>   contactsMap   = new HashMap<>();
    private Map<ID, List<ID>>   membersMap    = new HashMap<>();

    protected Facebook() {
        super();
    }

    //
    //  Meta
    //
    public boolean verify(Meta meta, ID identifier) {
        if (meta == null) {
            return false;
        }
        return meta.matches(identifier);
    }

    protected boolean cache(Meta meta, ID identifier) {
        if (!verify(meta, identifier)) {
            return false;
        }
        return super.cache(meta, identifier);
    }

    /**
     *  Save meta for entity ID
     *
     * @param meta - entity meta
     * @param identifier - entity ID
     * @return true on success
     */
    public abstract boolean saveMeta(Meta meta, ID identifier);

    /**
     *  Load meta for entity ID
     *
     * @param identifier - entity ID
     * @return Meta object on success
     */
    protected abstract Meta loadMeta(ID identifier);

    //
    //  Profile
    //
    public boolean verify(Profile profile, ID identifier) {
        if (identifier != null) {
            if (profile == null || profile.getIdentifier() != identifier) {
                // profile ID not match
                return false;
            }
        }
        return verify(profile);
    }
    public boolean verify(Profile profile) {
        if (profile == null) {
            return false;
        }
        if (profile.isValid()) {
            // already verified
            return true;
        }
        ID identifier = getID(profile.getIdentifier());
        if (identifier == null) {
            throw new NullPointerException("profile ID error: " + profile);
        }
        NetworkType type = identifier.getType();
        if (type.isUser() || type == NetworkType.Polylogue) {
            // if this is a user profile,
            //     verify it with the user's meta.key
            // else if this is a polylogue profile,
            //     verify it with the founder's meta.key (which equals to the group's meta.key)
            Meta meta = getMeta(identifier);
            if (meta == null) {
                return false;
            }
            return profile.verify(meta.key);
        } else {
            throw new UnsupportedOperationException("unsupported profile ID: " + profile);
        }
    }

    protected boolean cache(Profile profile, ID identifier) {
        if (profile == null) {
            // remove from cache if exists
            profileMap.remove(identifier);
            return false;
        }
        if (!verify(profile, identifier)) {
            return false;
        }
        // TODO: set expired time
        profileMap.put(identifier, profile);
        return true;
    }
    protected boolean cache(Profile profile) {
        if (profile == null) {
            return false;
        }
        ID identifier = getID(profile.getIdentifier());
        if (identifier == null) {
            throw new NullPointerException("profile ID error: " + profile);
        }
        return cache(profile, identifier);
    }

    /**
     *  Save profile with entity ID
     *
     * @param profile - entity profile
     * @return true on success
     */
    public abstract boolean saveProfile(Profile profile);

    /**
     *  Load profile for entity ID
     *
     * @param identifier - entity ID
     * @return Profile object on success
     */
    protected abstract Profile loadProfile(ID identifier);

    //
    //  Private Key
    //
    protected boolean cache(PrivateKey key, ID user) {
        assert key != null;
        privateKeyMap.put(user, key);
        return true;
    }

    /**
     *  Save private key for user ID
     *
     * @param key - private key
     * @param user - user ID
     * @return true on success
     */
    public abstract boolean savePrivateKey(PrivateKey key, ID user);

    /**
     *  Load private key for user ID
     *
     * @param user - user ID
     * @return PrivateKey object on success
     */
    protected abstract PrivateKey loadPrivateKey(ID user);

    //
    //  Relationship
    //
    private boolean cacheUsersList(List<ID> list, ID identifier) {
        NetworkType type = identifier.getType();
        if (type.isGroup()) {
            membersMap.put(identifier, list);
        } else if (type.isUser()) {
            contactsMap.put(identifier, list);
        } else {
            throw new IllegalArgumentException("entity ID not support: " + identifier);
        }
        return true;
    }

    //
    //  User contacts
    //
    protected boolean cacheContacts(List<ID> contacts, ID user) {
        assert user.getType().isUser();
        return cacheUsersList(contacts, user);
    }

    /**
     *  Save contacts for user
     *
     * @param contacts - contact list
     * @param user - user ID
     * @return true on success
     */
    public abstract boolean saveContacts(List<ID> contacts, ID user);

    /**
     *  Load contacts for user
     * @param user - user ID
     * @return contact list on success
     */
    protected abstract List<ID> loadContacts(ID user);

    //
    //  Group members
    //
    protected boolean cacheMembers(List<ID> members, ID group) {
        assert group.getType().isGroup();
        return cacheUsersList(members, group);
    }

    /**
     *  Save members of group
     *
     * @param members - member list
     * @param group - group ID
     * @return true on success
     */
    public abstract boolean saveMembers(List<ID> members, ID group);

    /**
     *  Load members of group
     *
     * @param group - group ID
     * @return member list on success
     */
    protected abstract List<ID> loadMembers(ID group);

    //----

    public String getNickname(ID identifier) {
        assert identifier.getType().isUser();
        User user = getUser(identifier);
        return user == null ? null : user.getName();
    }

    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format(Locale.CHINA, "%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }

    public ID getID(Address address) {
        ID identifier = new ID(null, address);
        Meta meta = getMeta(identifier);
        if (meta == null) {
            // failed to get meta for this ID
            return null;
        }
        String seed = meta.seed;
        if (seed == null) {
            return identifier;
        }
        identifier = new ID(seed, address);
        cache(identifier);
        return identifier;
    }

    //-------- SocialNetworkDataSource

    @Override
    public ID getID(Object string) {
        if (string == null) {
            return null;
        } else if (string instanceof ID) {
            return (ID) string;
        }
        assert string instanceof String;
        if (ans != null) {
            // try ANS record
            ID identifier = ans.identifier((String) string);
            if (identifier != null) {
                return identifier;
            }
        }
        // get from barrack
        return super.getID(string);
    }

    @Override
    public User getUser(ID identifier) {
        // get from barrack cache
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        // check meta and private key
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        NetworkType type = identifier.getType();
        if (type.isPerson()) {
            PrivateKey key = getPrivateKeyForSignature(identifier);
            if (key == null) {
                user = new User(identifier);
            } else {
                user = new LocalUser(identifier);
            }
        } else if (type.isStation()) {
            // FIXME: prevent station to be erased from memory cache
            user = new Station(identifier);
        } else {
            throw new UnsupportedOperationException("unsupported user type: " + type);
        }
        // cache it in barrack
        cache(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        // get from barrack cache
        Group group = super.getGroup(identifier);
        if (group != null) {
            return group;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type == NetworkType.Polylogue) {
            group = new Polylogue(identifier);
        } else if (type == NetworkType.Chatroom) {
            group = new Chatroom(identifier);
        }
        assert group != null;
        // cache it in barrack
        cache(group);
        return group;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = super.getMeta(identifier);
        if (meta == null) {
            meta = loadMeta(identifier);
            if (meta != null) {
                cache(meta, identifier);
            }
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = profileMap.get(identifier);
        if (profile == null) {
            profile = loadProfile(identifier);
            if (profile != null) {
                cache(profile, identifier);
            }
        }
        // TODO: check expired time
        return profile;
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = privateKeyMap.get(user);
        if (key == null) {
            key = loadPrivateKey(user);
            if (key != null) {
                cache(key, user);
            }
        }
        return key;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> keys = new ArrayList<>();
        // DIMP v1.0:
        //     decrypt key and the sign key are the same key
        PrivateKey key = getPrivateKeyForSignature(user);
        if (key != null) {
            keys.add(key);
        }
        return keys;
    }

    @Override
    public List<ID> getContacts(ID user) {
        List<ID> contacts;// = super.getContacts(identifier);
        assert user.getType().isUser();
        contacts = contactsMap.get(user);
        if (contacts == null) {
            contacts = loadContacts(user);
            if (contacts != null) {
                cacheContacts(contacts, user);
            }
        }
        return contacts;
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        ID founder = super.getFounder(group);
        if (founder != null) {
            return founder;
        }
        // check each member's public key with group meta
        List<ID> members = getMembers(group);
        if (members != null) {
            Meta meta = getMeta(group);
            if (meta != null) {
                // if the member's public key matches with the group's meta,
                // it means this meta was generate by the member's private key
                ID id;
                Meta m;
                for (Object item : members) {
                    id = getID(item);
                    assert id.getType().isUser();
                    m = getMeta(id);
                    if (m == null) {
                        continue;
                    }
                    if (meta.matches(m.key)) {
                        // got it
                        return id;
                    }
                }
            }
        }
        // TODO: load founder from database
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        ID owner = super.getOwner(group);
        if (owner != null) {
            return owner;
        }
        if (group.getType() == NetworkType.Polylogue) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        List<ID> members = super.getMembers(group);
        if (members != null) {
            return members;
        }
        assert group.getType().isGroup();
        members = membersMap.get(group);
        if (members == null) {
            members = loadMembers(group);
            if (members != null) {
                cacheMembers(members, group);
            }
        }
        return members;
    }

    public boolean isFounder(ID member, ID group) {
        // check member's public key with group's meta.key
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            throw new NullPointerException("failed to get meta for group: " + group);
        }
        Meta meta = getMeta(member);
        if (meta == null) {
            throw new NullPointerException("failed to get meta for member: " + member);
        }
        return gMeta.matches(meta.key);
    }

    public boolean isOwner(ID member, ID group) {
        if (group.getType() == NetworkType.Polylogue) {
            return isFounder(member, group);
        }
        throw new UnsupportedOperationException("only Polylogue so far");
    }

    public boolean existsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        for (ID item : members) {
            if (item.equals(member)) {
                return true;
            }
        }
        ID owner = getOwner(group);
        return owner == null || owner.equals(member);
    }

    //
    //  Group Assistants
    //
    public List<ID> getAssistants(ID group) {
        assert group.getType().isGroup();
        List<ID> assistants = new ArrayList<>();
        if (ans != null) {
            ID identifier = ans.identifier("assistant");
            if (identifier != null) {
                assistants.add(identifier);
            }
        }
        return assistants;
    }

    public boolean existsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        for (ID item : assistants) {
            if (item.equals(user)) {
                return true;
            }
        }
        return false;
    }
}
