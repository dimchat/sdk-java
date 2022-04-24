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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.mkm.Chatroom;
import chat.dim.mkm.Group;
import chat.dim.mkm.Polylogue;
import chat.dim.mkm.Robot;
import chat.dim.mkm.ServiceProvider;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;

public abstract class Facebook extends Barrack {

    // memory caches
    private final Map<ID, User> userMap = new HashMap<>();
    private final Map<ID, Group> groupMap = new HashMap<>();

    /**
     * Call it when received 'UIApplicationDidReceiveMemoryWarningNotification',
     * this will remove 50% of cached objects
     *
     * @return number of survivors
     */
    public int reduceMemory() {
        int finger = 0;
        finger = thanos(userMap, finger);
        finger = thanos(groupMap, finger);
        return finger >> 1;
    }

    public static <K, V> int thanos(Map<K, V> map, int finger) {
        Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next();
            if ((++finger & 1) == 1) {
                // kill it
                iterator.remove();
            }
            // let it go
        }
        return finger;
    }

    private void cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.identifier, user);
    }

    private void cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        groupMap.put(group.identifier, group);
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
     *  Save entity document with ID (must verify first)
     *
     * @param doc - entity document
     * @return true on success
     */
    public abstract boolean saveDocument(Document doc);

    /**
     *  Save members of group
     *
     * @param members - member ID list
     * @param group - group ID
     * @return true on success
     */
    public abstract boolean saveMembers(List<ID> members, ID group);

    /**
     *  Document checking
     *
     * @param doc - entity document
     * @return true on accepted
     */
    public boolean checkDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        if (identifier == null) {
            return false;
        }
        // NOTICE: if this is a bulletin document for group,
        //             verify it with the group owner's meta.key
        //         else (this is a visa document for user)
        //             verify it with the user's meta.key
        Meta meta;
        if (identifier.isGroup()) {
            // check by owner
            ID owner = getOwner(identifier);
            if (owner == null) {
                if (NetworkType.POLYLOGUE.equals(identifier.getType())) {
                    // NOTICE: if this is a polylogue document,
                    //             verify it with the founder's meta.key
                    //             (which equals to the group's meta.key)
                    meta = getMeta(identifier);
                } else {
                    // FIXME: owner not found for this group
                    return false;
                }
            } else {
                meta = getMeta(owner);
            }
        } else {
            meta = getMeta(identifier);
        }
        return meta != null && doc.verify(meta.getKey());
    }

    //-------- group membership

    public boolean isFounder(ID member, ID group) {
        // check member's public key with group's meta.key
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            // throw new AssertionError("failed to get meta for group: " + group);
            return false;
        }
        Meta mMeta = getMeta(member);
        if (mMeta == null) {
            // throw new AssertionError("failed to get meta for member: " + member);
            return false;
        }
        return Meta.matches(mMeta.getKey(), gMeta);
    }

    public boolean isOwner(ID member, ID group) {
        if (NetworkType.POLYLOGUE.equals(group.getType())) {
            return isFounder(member, group);
        }
        throw new UnsupportedOperationException("only Polylogue so far");
    }

    protected User createUser(ID identifier) {
        if (identifier.isBroadcast()) {
            // create user 'anyone@anywhere'
            return new User(identifier);
        }
        // make sure meta exists
        assert getMeta(identifier) != null : "meta not found for user: " + identifier;
        // TODO: make sure visa key exists before calling this
        // check user type
        byte type = identifier.getType();
        if (NetworkType.MAIN.equals(type) || NetworkType.BTC_MAIN.equals(type)) {
            return new User(identifier);
        }
        if (NetworkType.ROBOT.equals(type)) {
            return new Robot(identifier);
        }
        if (NetworkType.STATION.equals(type)) {
            return new Station(identifier);
        }
        throw new TypeNotPresentException("Unsupported user type: " + type, null);
    }

    protected Group createGroup(ID identifier) {
        if (identifier.isBroadcast()) {
            // create group 'everyone@everywhere'
            return new Group(identifier);
        }
        // make sure meta exists
        assert getMeta(identifier) != null : "meta not found for group: " + identifier;
        // check group type
        byte type = identifier.getType();
        if (NetworkType.POLYLOGUE.equals(type)) {
            return new Polylogue(identifier);
        }
        if (NetworkType.CHATROOM.equals(type)) {
            return new Chatroom(identifier);
        }
        if (NetworkType.PROVIDER.equals(type)) {
            return new ServiceProvider(identifier);
        }
        throw new TypeNotPresentException("Unsupported group type: " + type, null);
    }

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    public abstract List<User> getLocalUsers();

    /**
     *  Get current user (for signing and sending message)
     *
     * @return User
     */
    public User getCurrentUser() {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            return null;
        }
        return users.get(0);
    }

    /**
     *  Select local user for receiver
     *
     * @param receiver - user/group ID
     * @return local user
     */
    public User selectLocalUser(ID receiver) {
        List<User> users = getLocalUsers();
        if (users == null || users.size() == 0) {
            throw new NullPointerException("local users should not be empty");
        } else if (receiver.isBroadcast()) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        }
        if (receiver.isGroup()) {
            // group message (recipient not designated)
            List<ID> members = getMembers(receiver);
            if (members == null || members.size() == 0) {
                // TODO: group not ready, waiting for group info
                return null;
            }
            for (User item : users) {
                if (members.contains(item.identifier)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        } else {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.identifier)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        }
        return null;
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID identifier) {
        // 1. get from user cache
        User user = userMap.get(identifier);
        if (user == null) {
            // 2. create user and cache it
            user = createUser(identifier);
            if (user != null) {
                cache(user);
            }
        }
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        // 1. get from group cache
        Group group = groupMap.get(identifier);
        if (group == null) {
            // 2. create group and cache it
            group = createGroup(identifier);
            if (group != null) {
                cache(group);
            }
        }
        return group;
    }
}
