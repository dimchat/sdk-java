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
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;

public abstract class Messenger extends Transceiver {

    private Facebook facebook = null;

    private WeakReference<Transmitter> transmitterRef = null;

    protected Messenger() {
        super();
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
     *  Delegate for transmitting message
     *
     * @param transmitter - message transmitter
     */
    public void setTransmitter(Transmitter transmitter) {
        transmitterRef = new WeakReference<>(transmitter);
    }
    protected Transmitter getTransmitter() {
        return transmitterRef == null ? null : transmitterRef.get();
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

    /**
     *  Messenger Callback
     *  ~~~~~~~~~~~~~~~~~~
     */
    public interface Callback {

        void onSuccess();

        void onFailed(Error error);
    }
}
