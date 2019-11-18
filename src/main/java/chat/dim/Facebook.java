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

import java.lang.ref.WeakReference;
import java.util.*;

import chat.dim.core.Barrack;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.Address;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.Profile;

public abstract class Facebook extends Barrack {

    public static long EXPIRES = 3600;  // profile expires (1 hour)

    private WeakReference<AddressNameService> ansRef = null;

    // memory caches
    private Map<ID, Profile>    profileMap    = new HashMap<>();
    private Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, List<ID>>   contactsMap   = new HashMap<>();
    private Map<ID, List<ID>>   membersMap    = new HashMap<>();

    protected Facebook() {
        super();
    }

    public AddressNameService getANS() {
        if (ansRef == null) {
            return null;
        }
        return ansRef.get();
    }

    public void setANS(AddressNameService ans) {
        ansRef = new WeakReference<>(ans);
    }

    private ID ansGet(String name) {
        AddressNameService ans = getANS();
        if (ans == null) {
            return null;
        }
        return ans.identifier(name);
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

    @Override
    protected boolean cache(Meta meta, ID identifier) {
        if (!verify(meta, identifier)) {
            return false;
        }
        return super.cache(meta, identifier);
    }

    /**
     *  Save meta for entity ID (must verify first)
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
        ID identifier = getID(profile.getIdentifier());
        if (identifier == null) {
            throw new NullPointerException("profile ID error: " + profile);
        }
        Meta meta;
        if (identifier.getType().isGroup()) {
            // check by each member
            List<ID> members = getMembers(identifier);
            if (members == null || members.size() == 0) {
                throw new NullPointerException("members not found: " + identifier);
            }
            for (ID item : members) {
                meta = getMeta(item);
                if (meta != null && profile.verify(meta.getKey())) {
                    return true;
                }
            }
            // TODO: what to do about assistants?

            // check by owner
            ID owner = getOwner(identifier);
            if (owner == null) {
                throw new NullPointerException("owner not found: " + identifier);
            }
            if (members.contains(owner)) {
                // already checked
                return false;
            }
            meta = getMeta(owner);
        } else {
            assert identifier.getType().isUser();
            meta = getMeta(identifier);
        }
        return meta != null && profile.verify(meta.getKey());
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
     *  Save profile with entity ID (must verify first)
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
        if (key == null) {
            privateKeyMap.remove(user);
            return false;
        }
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
    //  User contacts
    //
    protected boolean cacheContacts(List<ID> contacts, ID user) {
        assert user.getType().isUser();
        if (contacts == null) {
            contactsMap.remove(user);
            return false;
        }
        contactsMap.put(user, contacts);
        return true;
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
        if (members == null) {
            membersMap.remove(group);
            return false;
        }
        membersMap.put(group, members);
        return true;
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

    public ID getID(Address address) {
        ID identifier = new ID(null, address);
        Meta meta = getMeta(identifier);
        if (meta == null) {
            // failed to get meta for this ID
            return null;
        }
        String seed = meta.getSeed();
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
        if (string instanceof String) {
            // try ANS record
            ID identifier = ansGet((String) string);
            if (identifier != null) {
                return identifier;
            }
        }
        // get from barrack
        return super.getID(string);
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = super.getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // load from local storage
        meta = loadMeta(identifier);
        if (meta == null) {
            return null;
        }
        cache(meta, identifier);
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = profileMap.get(identifier);
        if (profile != null) {
            // check expired time
            Date now = new Date();
            long timestamp = now.getTime() / 1000 + EXPIRES;
            Number expires = (Number) profile.get("expires");
            if (expires == null) {
                // set expired time
                profile.put("expires", timestamp);
                return profile;
            } else if (expires.longValue() < timestamp){
                // not expired yet
                return profile;
            }
        }
        // load from local storage
        profile = loadProfile(identifier);
        if (profile == null) {
            return null;
        }
        cache(profile, identifier);
        return profile;
    }

    //-------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        List<ID> contacts;// = super.getContacts(identifier);
        assert user.getType().isUser();
        contacts = contactsMap.get(user);
        if (contacts != null) {
            return contacts;
        }
        // load from local storage
        contacts = loadContacts(user);
        if (contacts == null) {
            return null;
        }
        cacheContacts(contacts, user);
        return contacts;
    }

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = privateKeyMap.get(user);
        if (key != null) {
            return key;
        }
        // load from local storage
        key = loadPrivateKey(user);
        if (key == null) {
            return null;
        }
        cache(key, user);
        return key;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        List<DecryptKey> keys = new ArrayList<>();
        // DIMP v1.0:
        //     decrypt key and the sign key are the same key
        PrivateKey key = getPrivateKeyForSignature(user);
        if (key != null) {
            // TODO: support profile.key
            assert key instanceof DecryptKey;
            keys.add((DecryptKey) key);
        }
        return keys;
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
                    if (meta.matches(m.getKey())) {
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
        if (members != null) {
            return members;
        }
        // load from local storage
        members = loadMembers(group);
        if (members == null) {
            return null;
        }
        cacheMembers(members, group);
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
        return gMeta.matches(meta.getKey());
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
        // try ANS record
        ID identifier = ansGet("assistant");
        if (identifier == null) {
            return null;
        }
        List<ID> assistants = new ArrayList<>();
        assistants.add(identifier);
        return assistants;
    }

    public boolean existsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        if (assistants == null) {
            return false;
        }
        return assistants.contains(user);
    }
}
