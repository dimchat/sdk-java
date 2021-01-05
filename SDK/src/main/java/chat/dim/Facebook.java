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

import chat.dim.core.Barrack;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.network.Robot;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Station;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;

public abstract class Facebook extends Barrack {

    protected Facebook() {
        super();
    }

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

    //
    //  Document checking
    //
    public boolean isEmpty(Document doc) {
        if (doc == null) {
            return true;
        }
        String json = (String) doc.get("data");
        return json == null || json.length() == 0;
    }

    public boolean isValid(Document doc) {
        ID identifier = doc.getIdentifier();
        if (identifier == null) {
            return false;
        }
        // NOTICE: if this is a group profile,
        //             verify it with each member's meta.key
        //         else (this is a user profile)
        //             verify it with the user's meta.key
        Meta meta;
        if (identifier.isGroup()) {
            // check by each member
            List<ID> members = getMembers(identifier);
            if (members != null) {
                for (ID item : members) {
                    meta = getMeta(item);
                    if (meta == null) {
                        // FIXME: meta not found for this member
                        continue;
                    }
                    if (doc.verify(meta.getKey())) {
                        return true;
                    }
                }
            }
            // DISCUSS: what to do about assistants?

            // check by owner
            ID owner = getOwner(identifier);
            if (owner == null) {
                if (NetworkType.POLYLOGUE.equals(identifier.getType())) {
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
        return meta != null && doc.verify(meta.getKey());
    }

    @Override
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

    @Override
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
}
