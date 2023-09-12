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
import java.util.List;
import java.util.Map;

import chat.dim.mkm.AddressFactory;
import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Bot;
import chat.dim.mkm.Group;
import chat.dim.mkm.ServiceProvider;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

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
        finger = AddressFactory.thanos(userMap, finger);
        finger = AddressFactory.thanos(groupMap, finger);
        return finger >> 1;
    }

    private void cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.getIdentifier(), user);
    }

    private void cache(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        groupMap.put(group.getIdentifier(), group);
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
     *  Create user when visa.key exists
     *
     * @param identifier - user ID
     * @return user, null on not ready
     */
    protected User createUser(ID identifier) {
        // check visa key
        if (!identifier.isBroadcast()) {
            if (getPublicKeyForEncryption(identifier) == null) {
                assert false : "visa.key not found: " + identifier;
                return null;
            }
            // NOTICE: if visa.key exists, then visa & meta must exist.
        }
        int type = identifier.getType();
        // check user type
        if (EntityType.STATION.equals(type)) {
            return new Station(identifier);
        } else if (EntityType.BOT.equals(type)) {
            return new Bot(identifier);
        }
        // general user, or 'anyone@anywhere'
        return new BaseUser(identifier);
    }

    /**
     *  Create group when meta & members exist
     *
     * @param identifier - group ID
     * @return group, null on not ready
     */
    protected Group createGroup(ID identifier) {
        // check meta & members
        if (!identifier.isBroadcast()) {
            if (getMeta(identifier) == null) {
                assert false : "meta not found: " + identifier;
                return null;
            }
            List<ID> members = getMembers(identifier);
            if (members == null || members.size() == 0) {
                assert false : "group members not found: " + identifier;
                return null;
            }
            // NOTICE: if members exist, then owner exists,
            //         and founder, bulletin must exist too.
        }
        int type = identifier.getType();
        // check group type
        if (EntityType.ISP.equals(type)) {
            return new ServiceProvider(identifier);
        }
        // general group, or 'everyone@everywhere'
        return new BaseGroup(identifier);
    }

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    public abstract List<User> getLocalUsers();

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
        } else if (receiver.isUser()) {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.getIdentifier())) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
            return null;
        }
        // group message (recipient not designated)
        assert receiver.isGroup() : "receiver error: " + receiver;
        // the messenger will check group info before decrypting message,
        // so we can trust that the group's meta & members MUST exist here.
        Group grp = getGroup(receiver);
        if (grp == null) {
            assert false : "group not ready: " + receiver;
            return null;
        }
        // if a group can be create, means its meta, bulletin document,
        // and owner, members are all ready, so members should not be empty here.
        List<ID> members = grp.getMembers();
        assert members.size() > 0 : "members not found: " + receiver;
        for (User item : users) {
            if (members.contains(item.getIdentifier())) {
                // DISCUSS: set this item to be current user?
                return item;
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
