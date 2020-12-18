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

import chat.dim.core.Packer;
import chat.dim.core.Transceiver;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.FileContentProcessor;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.Document;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public class Messenger extends Transceiver {

    private WeakReference<Delegate> delegateRef = null;
    private WeakReference<DataSource> dataSourceRef = null;

    private Packer messagePacker = null;
    private MessageProcessor messageProcessor = null;
    private MessageTransmitter messageTransmitter = null;

    public Messenger() {
        super();
    }

    /**
     *  Delegate for sending data
     *
     * @param delegate - message delegate
     */
    public void setDelegate(Delegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }
    protected Delegate getDelegate() {
        return delegateRef.get();
    }

    /**
     *  Delegate for saving message
     *
     * @param delegate - message data source
     */
    public void setDataSource(DataSource delegate) {
        dataSourceRef = new WeakReference<>(delegate);
    }
    protected DataSource getDataSource() {
        return dataSourceRef.get();
    }

    /**
     *  Delegate for getting entity info
     *
     * @param delegate - entity data source
     */
    public void setFacebook(Facebook delegate) {
        setEntityDelegate(delegate);
    }
    public Facebook getFacebook() {
        return (Facebook) getEntityDelegate();
    }

    //
    //  Message Packer
    //
    protected Packer getMessagePacker() {
        if (messagePacker == null) {
            messagePacker = newMessagePacker();
        }
        return messagePacker;
    }
    protected Packer newMessagePacker() {
        return new Packer(getEntityDelegate(), this, getCipherKeyDelegate());
    }

    //
    //  Message Processor
    //
    protected MessageProcessor getMessageProcessor() {
        if (messageProcessor == null) {
            messageProcessor = newMessageProcessor();
        }
        return messageProcessor;
    }
    protected MessageProcessor newMessageProcessor() {
        return new MessageProcessor(getFacebook(), this, getMessagePacker());
    }

    //
    //  Message Transmitter
    //
    protected MessageTransmitter getMessageTransmitter() {
        if (messageTransmitter == null) {
            messageTransmitter = newMessageTransmitter();
        }
        return messageTransmitter;
    }
    protected MessageTransmitter newMessageTransmitter() {
        return new MessageTransmitter(getFacebook(), this, getMessagePacker());
    }

    private FileContentProcessor getFileContentProcessor() {
        ContentProcessor cpu = ContentProcessor.getProcessor(ContentType.FILE);
        cpu.setMessenger(this);
        return (FileContentProcessor) cpu;
    }

    //-------- InstantMessageDelegate

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.uploadFileContent((FileContent) content, password, iMsg);
        }
        return super.serializeContent(content, password, iMsg);
    }

    private EncryptKey getPublicKeyForEncryption(ID receiver) {
        Facebook facebook = getFacebook();
        Document doc = facebook.getDocument(receiver, Document.VISA);
        if (doc instanceof Visa) {
            EncryptKey key = ((Visa) doc).getKey();
            if (key != null) {
                return key;
            }
        }
        Meta meta = facebook.getMeta(receiver);
        if (meta == null) {
            return null;
        }
        VerifyKey key = meta.getKey();
        if (key instanceof EncryptKey) {
            return (EncryptKey) key;
        }
        return null;
    }

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        EncryptKey key = getPublicKeyForEncryption(receiver);
        if (key == null) {
            // save this message in a queue waiting receiver's meta/document response
            getDataSource().suspendMessage(iMsg);
            //throw new NullPointerException("failed to get encrypt key for receiver: " + receiver);
            return null;
        }
        return super.encryptKey(data, receiver, iMsg);
    }

    //-------- SecureMessageDelegate

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content = super.deserializeContent(data, password, sMsg);
        if (content == null) {
            throw new NullPointerException("failed to deserialize message content: " + sMsg);
        }
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.downloadFileContent((FileContent) content, password, sMsg);
        }
        return content;
    }

    //
    //  Interfaces for Processing Message
    //
    public byte[] process(byte[] data) {
        return getMessageProcessor().process(data);
    }

    public ReliableMessage process(ReliableMessage rMsg) {
        return getMessageProcessor().process(rMsg);
    }

    //
    //  Interfaces for Sending Message
    //
    public boolean sendContent(Content content, ID receiver, Messenger.Callback callback, int priority) {
        return getMessageTransmitter().sendContent(content, receiver, callback, priority);
    }

    public boolean sendMessage(InstantMessage iMsg, Messenger.Callback callback, int priority) {
        return getMessageTransmitter().sendMessage(iMsg, callback, priority);
    }

    public boolean sendMessage(ReliableMessage rMsg, Messenger.Callback callback, int priority) {
        return getMessageTransmitter().sendMessage(rMsg, callback, priority);
    }

    //
    //  Interfaces for Packing Message
    //
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        return getMessagePacker().encryptMessage(iMsg);
    }

    public ReliableMessage signMessage(SecureMessage sMsg) {
        return getMessagePacker().signMessage(sMsg);
    }

    public byte[] serializeMessage(ReliableMessage rMsg) {
        return getMessagePacker().serializeMessage(rMsg);
    }

    public ReliableMessage deserializeMessage(byte[] data) {
        return getMessagePacker().deserializeMessage(data);
    }

    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        return getMessagePacker().verifyMessage(rMsg);
    }

    public InstantMessage decryptMessage(SecureMessage sMsg) {
        return getMessagePacker().decryptMessage(sMsg);
    }

    //
    //  Interfaces for Station
    //
    public String uploadData(byte[] data, InstantMessage iMsg) {
        return getDelegate().uploadData(data, iMsg);
    }

    public byte[] downloadData(String url, InstantMessage iMsg) {
        return getDelegate().downloadData(url, iMsg);
    }

    public boolean sendPackage(byte[] data, CompletionHandler handler, int priority) {
        return getDelegate().sendPackage(data, handler, priority);
    }

    //
    //  Interfaces for Message Storage
    //
    public boolean saveMessage(InstantMessage iMsg) {
        return getDataSource().saveMessage(iMsg);
    }

    public void suspendMessage(ReliableMessage rMsg) {
        getDataSource().suspendMessage(rMsg);
    }

    public void suspendMessage(InstantMessage iMsg) {
        getDataSource().suspendMessage(iMsg);
    }

    /**
     *  Messenger Delegate
     *  ~~~~~~~~~~~~~~~~~~
     */
    public interface Delegate {

        /**
         *  Upload encrypted data to CDN
         *
         * @param data - encrypted file data
         * @param iMsg - instant message
         * @return download URL
         */
        String uploadData(byte[] data, InstantMessage iMsg);

        /**
         *  Download encrypted data from CDN
         *
         * @param url - download URL
         * @param iMsg - instant message
         * @return encrypted file data
         */
        byte[] downloadData(String url, InstantMessage iMsg);

        /**
         *  Send out a data package onto network
         *
         * @param data - package data
         * @param handler - completion handler
         * @return true on success
         */
        boolean sendPackage(byte[] data, CompletionHandler handler, int priority);
    }

    /**
     *  Messenger DataSource
     *  ~~~~~~~~~~~~~~~~~~~~
     */
    public interface DataSource {

        /**
         * Save the message into local storage
         *
         * @param iMsg - instant message
         * @return true on success
         */
        boolean saveMessage(InstantMessage iMsg);

        /**
         *  Suspend the received message for the sender's meta
         *
         * @param rMsg - message received from network
         */
        void suspendMessage(ReliableMessage rMsg);

        /**
         *  Suspend the sending message for the receiver's meta,
         *  or group meta when received new message
         *
         * @param iMsg - instant message to be sent
         */
        void suspendMessage(InstantMessage iMsg);
    }

    /**
     *  Messenger Callback
     *  ~~~~~~~~~~~~~~~~~~
     */
    public interface Callback {

        void onFinished(Object result, Error error);
    }

    /**
     *  Messenger Completion Handler
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    public interface CompletionHandler {

        void onSuccess();

        void onFailed(Error error);
    }
}
