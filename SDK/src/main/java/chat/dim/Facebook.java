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

import java.util.List;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.BroadcastAddress;
import chat.dim.network.Robot;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Station;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Profile;

public abstract class Facebook extends Barrack {

    protected Facebook() {
        super();
    }

    //
    //  Meta
    //
    public boolean verify(Meta meta, ID identifier) {
        assert meta != null && meta.isValid() : "meta error: " + meta;
        return meta.matches(identifier);
    }

    /**
     *  Save meta for entity ID (must verify first)
     *
     * @param meta - entity meta
     * @param identifier - entity ID
     * @return true on success
     */
    public abstract boolean saveMeta(Meta meta, ID identifier);

    //
    //  Profile
    //
    public boolean isEmpty(Profile profile) {
        if (profile == null) {
            return true;
        }
        if (profile instanceof Map) {
            String json = (String) ((Map) profile).get("data");
            return json == null || json.length() == 0;
        }
        return true;
    }

    public boolean verify(Profile profile, ID identifier) {
        if (identifier != null) {
            if (profile == null || !identifier.equals(profile.getIdentifier())) {
                // profile ID not match
                return false;
            }
        }
        return verify(profile);
    }
    public boolean verify(Profile profile) {
        assert profile != null : "profile should not be empty";
        ID identifier = profile.getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("profile ID error: " + profile);
        }
        // NOTICE: if this is a group profile,
        //             verify it with each member's meta.key
        //         else (this is a user profile)
        //             verify it with the user's meta.key
        Meta meta;
        if (NetworkType.isGroup(identifier.getType())) {
            // check by each member
            List<ID> members = getMembers(identifier);
            if (members != null) {
                for (ID item : members) {
                    meta = getMeta(item);
                    if (meta == null) {
                        // FIXME: meta not found for this member
                        continue;
                    }
                    if (profile.verify(meta.getKey())) {
                        return true;
                    }
                }
            }
            // DISCUSS: what to do about assistants?

            // check by owner
            ID owner = getOwner(identifier);
            if (owner == null) {
                if (NetworkType.Polylogue.equals(identifier.getType())) {
                    // NOTICE: if this is a polylogue profile
                    //             verify it with the founder's meta.key
                    //             (which equals to the group's meta.key)
                    meta = getMeta(identifier);
                } else {
                    // FIXME: owner not found for this group
                    return false;
                }
            } else if (members != null && members.contains(owner)) {
                // already checked
                return false;
            } else {
                meta = getMeta(owner);
            }
        } else {
            meta = getMeta(identifier);
        }
        return meta != null && profile.verify(meta.getKey());
    }

    /**
     *  Save profile with entity ID (must verify first)
     *
     * @param profile - entity profile
     * @return true on success
     */
    public abstract boolean saveProfile(Profile profile);

    /**
     *  Save members of group
     *
     * @param members - member ID list
     * @param group - group ID
     * @return true on success
     */
    public abstract boolean saveMembers(List<ID> members, ID group);

    //-------- Local Users

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return user list
     */
    public abstract List<User> getLocalUsers();

    /**
     *  Get current user (for signing and sending message)
     *
     * @return user object
     */
    public User getCurrentUser() {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    //--------

    @Override
    protected User createUser(ID identifier) {
        if (identifier.getAddress() instanceof BroadcastAddress) {
            // create user 'anyone@anywhere'
            return new User(identifier);
        }
        // make sure meta exists
        assert getMeta(identifier) != null : "meta not found for user: " + identifier;
        // check user type
        byte type = identifier.getType();
        if (NetworkType.Main.equals(type) || NetworkType.BTCMain.equals(type)) {
            return new User(identifier);
        }
        if (NetworkType.Robot.equals(type)) {
            return new Robot(identifier);
        }
        if (NetworkType.Station.equals(type)) {
            return new Station(identifier);
        }
        throw new TypeNotPresentException("Unsupported user type: " + type, null);
    }

    @Override
    protected Group createGroup(ID identifier) {
        if (identifier.getAddress() instanceof BroadcastAddress) {
            // create group 'everyone@everywhere'
            return new Group(identifier);
        }
        // make sure meta exists
        assert getMeta(identifier) != null : "meta not found for group: " + identifier;
        // check group type
        byte type = identifier.getType();
        if (NetworkType.Polylogue.equals(type)) {
            return new Polylogue(identifier);
        }
        if (NetworkType.Chatroom.equals(type)) {
            return new Chatroom(identifier);
        }
        if (NetworkType.Provider.equals(type)) {
            return new ServiceProvider(identifier);
        }
        throw new TypeNotPresentException("Unsupported group type: " + type, null);
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
                Meta m;
                for (ID item : members) {
                    m = getMeta(item);
                    if (m == null) {
                        continue;
                    }
                    if (meta.matches(m.getKey())) {
                        // got it
                        return item;
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
        // check group type
        if (NetworkType.Polylogue.equals(group.getType())) {
            // Polylogue's owner is its founder
            return getFounder(group);
        }
        // TODO: load owner from database
        return null;
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
        if (NetworkType.Polylogue.equals(group.getType())) {
            return isFounder(member, group);
        }
        throw new UnsupportedOperationException("only Polylogue so far");
    }

    public boolean existsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members != null && members.contains(member)) {
            return true;
        }
        ID owner = getOwner(group);
        return owner != null && owner.equals(member);
    }

    //
    //  Group Assistants
    //

    /**
     *  Get assistants for this group
     *
     * @param group - group ID
     * @return robot ID list
     */
    public abstract List<ID> getAssistants(ID group);

    public boolean existsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        if (assistants == null) {
            return false;
        }
        return assistants.contains(user);
    }
}
