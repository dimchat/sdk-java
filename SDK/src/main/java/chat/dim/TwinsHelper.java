/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.mkm.User;
import chat.dim.protocol.ID;


/**
 *  Base helper class that provides unified access to Facebook and Messenger dependencies.
 *  <p>
 *      "Twins" refers to the paired core services:
 *      - <b>Facebook</b>: Entity management (user/group metadata, local user selection)
 *      - <b>Messenger</b>: Messaging core (packing/unpacking, encryption/decryption, key management)
 *  </p>
 *  <p>
 *      Key design features:
 *      1. Uses <b>WeakReference</b> to hold dependencies, preventing memory leaks (avoids circular references)
 *      2. Provides a unified entry point for local user selection (critical for message decryption)
 *      3. Serves as the parent class for all core messaging components (Packer/Processor/ContentProcessor)
 *  </p>
 *  <p>
 *      All subclasses inherit access to Facebook/Messenger and the local user selection logic,
 *      ensuring consistent dependency management across the messaging system.
 *  </p>
 */
public class TwinsHelper {

    private final WeakReference<Facebook> facebookRef;
    private final WeakReference<Messenger> messengerRef;

    /**
     *  Creates a {@link TwinsHelper} with references to the core Facebook and Messenger services.
     *  <p>
     *      Note: Uses WeakReference to store dependencies to avoid memory leaks.
     *  </p>
     *
     * @param facebook is the entity management service (user/group operations).
     * @param messenger is the core messaging service (packing/processing/key management).
     */
    public TwinsHelper(Facebook facebook, Messenger messenger) {
        super();
        facebookRef = new WeakReference<>(facebook);
        messengerRef = new WeakReference<>(messenger);
    }

    /**
     *  Retrieves the Facebook service instance (nullable - may be GC'd).
     *
     * @return the facebook instance (null if garbage collected or not initialized).
     */
    protected Facebook getFacebook() {
        return facebookRef.get();
    }

    /**
     *  Retrieves the Messenger service instance (nullable - may be GC'd).
     *
     * @return the messenger instance (null if garbage collected or not initialized).
     */
    protected Messenger getMessenger() {
        return messengerRef.get();
    }

    /**
     *  Selects the local User entity for decrypting messages to a target receiver (unified entry).
     *  <p>
     *      Orchestration logic (receiver type routing):
     *      1. Broadcast receiver &rarr; use {@link Facebook#selectUser(ID)} (any local user can decrypt)
     *      2. User receiver &rarr; use {@link Facebook#selectUser(ID)} (matching local user for personal message)
     *      3. Group receiver &rarr;
     *          a. Get group members via Facebook (guaranteed to exist per precondition)
     *          b. Use {@link Facebook#selectMember(List)} (find local user in group member list)
     *      4. Convert selected user ID to full User entity (via {@link Facebook#getUser(ID)})
     *  </p>
     *  <p>
     *      Precondition: Group member list is guaranteed to exist
     *  </p>
     *
     * @param receiver is the target receiver ID (supports broadcast/user/group types).
     * @return the local User entity for decryption (null if no matching local user found).
     * @throws AssertionError when:
     *      - Facebook service is unavailable (null)
     *      - Receiver type is invalid (not broadcast/user/group)
     *      - Group member list is empty/missing (violates precondition)
     */
    protected User selectLocalUser(ID receiver) {
        Facebook facebook = getFacebook();
        assert facebook != null : "facebook not ready";
        ID me;
        if (receiver.isBroadcast()) {
            // broadcast message can be decrypted by anyone
            me = facebook.selectUser(receiver);
        } else if (receiver.isUser()) {
            // check local users
            me = facebook.selectUser(receiver);
        } else if (receiver.isGroup()) {
            // check local users for the group members
            List<ID> members = facebook.getMembers(receiver);
            // the messenger will check group info before decrypting message,
            // so we can trust that the group's meta & members MUST exist here.
            if (members == null || members.isEmpty()) {
                assert false : "failed to get group members: " + receiver;
                return null;
            }
            me = facebook.selectMember(members);
        } else {
            assert false : "unknown receiver: " + receiver;
            return null;
        }
        if (me == null) {
            // not for me?
            return null;
        }
        return facebook.getUser(me);
    }

    //
    //  Mapping
    //

    /**
     *  Create a new map with key values
     *
     * @param keyValues - key1, value1, key2, value2, ...
     * @return map
     */
    public static Map<String, Object> newMap(Object... keyValues) {
        Map<String, Object> info = new HashMap<>();
        Object key, value;
        for (int i = 1; i < keyValues.length; i += 2) {
            key = keyValues[i - 1];
            value = keyValues[i];
            if (key == null || value == null) {
                assert value == null : "map key should not be empty";
                continue;
            } else {
                assert key instanceof String : "key error: " + key;
            }
            info.put((String) key, value);
        }
        return info;
    }

}
