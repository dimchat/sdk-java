/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim.core;

import java.util.Arrays;

import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.Entity;
import chat.dim.mkm.User;
import chat.dim.msg.BaseMessage;
import chat.dim.msg.InstantMessageDelegate;
import chat.dim.msg.ReliableMessageDelegate;
import chat.dim.msg.SecureMessageDelegate;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

/**
 *  Message Transceiver
 *  ~~~~~~~~~~~~~~~~~~~
 *
 *  Converting message format between PlainMessage and NetworkMessage
 */
public abstract class Transceiver implements InstantMessageDelegate, SecureMessageDelegate, ReliableMessageDelegate {

    protected abstract Entity.Delegate getFacebook();

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // NOTICE: check attachment for File/Image/Audio/Video message content
        //         before serialize content, this job should be do in subclass
        return UTF8.encode(JSON.encode(content.toMap()));
    }

    @Override
    public byte[] encryptContent(byte[] data, SymmetricKey password, InstantMessage iMsg) {
        // store 'IV' in iMsg for AES decryption
        return password.encrypt(data, iMsg.toMap());
    }

    /*/
    @Override
    public Object encodeData(byte[] data, InstantMessage iMsg) {
        if (BaseMessage.isBroadcast(iMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so no need to encode to Base64 here
            return UTF8.decode(data);
        }
        // message content had been encrypted by a symmetric key,
        // so the data should be encoded here (with algorithm 'base64' as default).
        return TransportableData.encode(data);
    }
    /*/

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        if (BaseMessage.isBroadcast(iMsg)) {
            // broadcast message has no key
            return null;
        }
        return UTF8.encode(JSON.encode(password.toMap()));
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        assert !BaseMessage.isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        Entity.Delegate facebook = getFacebook();
        assert facebook != null : "entity delegate not set yet";
        // TODO: make sure the receiver's public key exists
        assert receiver.isUser() : "receiver error: " + receiver;
        User contact = facebook.getUser(receiver);
        if (contact == null) {
            assert false : "failed to encrypt message key for contact: " + receiver;
            return null;
        }
        // encrypt with public key of the receiver (or group member)
        return contact.encrypt(data);
    }

    /*/
    @Override
    public Object encodeKey(byte[] data, InstantMessage iMsg) {
        assert !BaseMessage.isBroadcast(iMsg) : "broadcast message has no key: " + iMsg;
        // message key had been encrypted by a public key,
        // so the data should be encode here (with algorithm 'base64' as default).
        return TransportableData.encode(data);
    }
    /*/

    //-------- SecureMessageDelegate

    /*/
    @Override
    public byte[] decodeKey(Object key, SecureMessage sMsg) {
        assert !BaseMessage.isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        return TransportableData.decode(key);
    }
    /*/

    @Override
    public byte[] decryptKey(byte[] key, ID receiver, SecureMessage sMsg) {
        // NOTICE: the receiver must be a member ID
        //         if it's a group message
        assert !BaseMessage.isBroadcast(sMsg) : "broadcast message has no key: " + sMsg;
        Entity.Delegate facebook = getFacebook();
        assert facebook != null : "entity delegate not set yet";
        assert receiver.isUser() : "receiver error: " + receiver;
        User user = facebook.getUser(receiver);
        if (user == null) {
            assert false : "failed to decrypt key: " + sMsg.getSender() + " => " + receiver + ", " + sMsg.getGroup();
            return null;
        }
        // decrypt with private key of the receiver (or group member)
        return user.decrypt(key);
    }

    @Override
    public SymmetricKey deserializeKey(byte[] key, SecureMessage sMsg) {
        assert !BaseMessage.isBroadcast(sMsg) : "broadcast message has no key: " + sMsg.toMap();
        if (key == null) {
            assert false : "reused key? get it from local cache: "
                    + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.getGroup();
            return null;
        }
        String json = UTF8.decode(key);
        if (json == null) {
            assert false : "message key data error: " + Arrays.toString(key);
            return null;
        }
        Object dict = JSON.decode(json);
        // TODO: translate short keys
        //       'A' -> 'algorithm'
        //       'D' -> 'data'
        //       'V' -> 'iv'
        //       'M' -> 'mode'
        //       'P' -> 'padding'
        return SymmetricKey.parse(dict);
    }

    /*/
    @Override
    public byte[] decodeData(Object data, SecureMessage sMsg) {
        if (BaseMessage.isBroadcast(sMsg)) {
            // broadcast message content will not be encrypted (just encoded to JsON),
            // so return the string data directly
            if (data instanceof String) {
                return UTF8.encode((String) data);
            } else {
                assert false : "content data error: " + data;
                return null;
            }
        }
        // message content had been encrypted by a symmetric key,
        // so the data should be encoded here (with algorithm 'base64' as default).
        return TransportableData.decode(data);
    }
    /*/

    @Override
    public byte[] decryptContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        // check 'IV' in sMsg for AES decryption
        return password.decrypt(data, sMsg.toMap());
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        //assert sMsg.getData() != null : "message data empty";
        String json = UTF8.decode(data);
        if (json == null) {
            assert false : "content data error: " + Arrays.toString(data);
            return null;
        }
        Object dict = JSON.decode(json);
        // TODO: translate short keys
        //       'T' -> 'type'
        //       'N' -> 'sn'
        //       'W' -> 'time'
        //       'G' -> 'group'
        return Content.parse(dict);
    }

    @Override
    public byte[] signData(byte[] data, SecureMessage sMsg) {
        Entity.Delegate facebook = getFacebook();
        assert facebook != null : "entity delegate not set yet";
        ID sender = sMsg.getSender();
        User user = facebook.getUser(sender);
        if (user == null) {
            assert false : "failed to sign message data for user: " + sender;
            return null;
        }
        return user.sign(data);
    }

    /*/
    @Override
    public Object encodeSignature(byte[] signature, SecureMessage sMsg) {
        return TransportableData.encode(signature);
    }
    /*/

    //-------- ReliableMessageDelegate


    /*/
    @Override
    public byte[] decodeSignature(Object signature, ReliableMessage rMsg) {
        return TransportableData.decode(signature);
    }
    /*/

    @Override
    public boolean verifyDataSignature(byte[] data, byte[] signature, ReliableMessage rMsg) {
        Entity.Delegate facebook = getFacebook();
        assert facebook != null : "entity delegate not set yet";
        ID sender = rMsg.getSender();
        User contact = facebook.getUser(sender);
        if (contact == null) {
            assert false : "failed to verify message signature for contact: " + sender;
            return false;
        }
        return contact.verify(data, signature);
    }
}
