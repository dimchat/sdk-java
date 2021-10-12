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

import chat.dim.core.Transceiver;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.FileContentProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public abstract class Messenger extends Transceiver {

    private WeakReference<Delegate> delegateRef = null;

    private WeakReference<Transmitter> transmitterRef = null;

    private Facebook facebook = null;
    private MessagePacker messagePacker = null;
    private MessageProcessor messageProcessor = null;
    private MessageTransmitter messageTransmitter = null;

    protected Messenger() {
        super();
    }

    /**
     *  Delegate for Station
     *
     * @param delegate - message delegate
     */
    public void setDelegate(Delegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }
    protected Delegate getDelegate() {
        return delegateRef == null ? null : delegateRef.get();
    }

    /**
     *  Delegate for getting entity
     *
     * @param barrack - facebook
     */
    @Override
    public void setEntityDelegate(Entity.Delegate barrack) {
        super.setEntityDelegate(barrack);
        if (barrack instanceof Facebook) {
            facebook = (Facebook) barrack;
        }
    }
    @Override
    protected Entity.Delegate getEntityDelegate() {
        Entity.Delegate delegate = super.getEntityDelegate();
        if (delegate == null) {
            delegate = getFacebook();
            super.setEntityDelegate(delegate);
        }
        return delegate;
    }
    public Facebook getFacebook() {
        if (facebook == null) {
            facebook = createFacebook();
        }
        return facebook;
    }
    protected abstract Facebook createFacebook();

    /**
     *  Delegate for packing message
     *
     * @param packer - message packer
     */
    @Override
    public void setPacker(Packer packer) {
        super.setPacker(packer);
        if (packer instanceof MessagePacker) {
            messagePacker = (MessagePacker) packer;
        }
    }
    @Override
    protected Packer getPacker() {
        Packer packer = super.getPacker();
        if (packer == null) {
            packer = getMessagePacker();
            super.setPacker(packer);
        }
        return packer;
    }
    private MessagePacker getMessagePacker() {
        if (messagePacker == null) {
            messagePacker = createMessagePacker();
        }
        return messagePacker;
    }
    protected MessagePacker createMessagePacker() {
        return new MessagePacker(this);
    }

    /**
     *  Delegate for processing message
     *
     * @param processor - message processor
     */
    @Override
    public void setProcessor(Processor processor) {
        super.setProcessor(processor);
        if (processor instanceof MessageProcessor) {
            messageProcessor = (MessageProcessor) processor;
        }
    }
    @Override
    protected Processor getProcessor() {
        Processor processor = super.getProcessor();
        if (processor == null) {
            processor = getMessageProcessor();
            super.setProcessor(processor);
        }
        return processor;
    }
    private MessageProcessor getMessageProcessor() {
        if (messageProcessor == null) {
            messageProcessor = createMessageProcessor();
        }
        return messageProcessor;
    }
    protected MessageProcessor createMessageProcessor() {
        return new MessageProcessor(this);
    }

    /**
     *  Delegate for transmitting message
     *
     * @param transmitter - message transmitter
     */
    public void setTransmitter(Transmitter transmitter) {
        transmitterRef = new WeakReference<>(transmitter);
        if (transmitter instanceof MessageTransmitter) {
            messageTransmitter = (MessageTransmitter) transmitter;
        }
    }
    protected Transmitter getTransmitter() {
        Transmitter transmitter = transmitterRef == null ? null : transmitterRef.get();
        if (transmitter == null) {
            transmitter = getMessageTransmitter();
            transmitterRef = new WeakReference<>(transmitter);
        }
        return transmitter;
    }
    private MessageTransmitter getMessageTransmitter() {
        if (messageTransmitter == null) {
            messageTransmitter = createMessageTransmitter();
        }
        return messageTransmitter;
    }
    protected MessageTransmitter createMessageTransmitter() {
        return new MessageTransmitter(this);
    }

    private FileContentProcessor getFileContentProcessor() {
        ContentProcessor cpu = ContentProcessor.getProcessor(ContentType.FILE);
        assert cpu instanceof FileContentProcessor : "failed to get file content processor";
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
    //  Interfaces for transmitting Message
    //
    public boolean sendContent(ID sender, ID receiver, Content content, Messenger.Callback callback, int priority) {
        return getTransmitter().sendContent(sender, receiver, content, callback, priority);
    }

    public boolean sendMessage(InstantMessage iMsg, Messenger.Callback callback, int priority) {
        return getTransmitter().sendMessage(iMsg, callback, priority);
    }

    public boolean sendMessage(ReliableMessage rMsg, Messenger.Callback callback, int priority) {
        return getTransmitter().sendMessage(rMsg, callback, priority);
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
         * @param priority - task priority
         * @return true on success
         */
        boolean sendPackage(byte[] data, CompletionHandler handler, int priority);
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
