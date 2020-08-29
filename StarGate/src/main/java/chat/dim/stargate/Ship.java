/* license: https://mit-license.org
 *
 *  Star Gate: Interfaces for network connection
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.stargate;

import java.lang.ref.WeakReference;

import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.protocol.TransactionID;

class Ship implements chat.dim.sg.Ship<TransactionID, Package> {

    private final Passenger passenger;
    private final WeakReference<Delegate> delegateRef;

    Ship(Passenger passenger, Delegate delegate) {
        super();
        this.passenger = passenger;
        delegateRef = new WeakReference<>(delegate);
    }

    int getPriority() {
        return getPassenger().priority;
    }

    Package getPackage() {
        return getPassenger().getPackage();
    }

    TransactionID getTransactionID() {
        return getPassenger().getID();
    }

    byte[] getRequestData() {
        return getPassenger().getRequestData();
    }

    //
    //  Ship
    //

    @Override
    public Passenger getPassenger() {
        return passenger;
    }

    @Override
    public Delegate getDelegate() {
        return delegateRef.get();
    }

    //
    //  StarDelegate
    //

    interface Delegate extends chat.dim.sg.StarDelegate<TransactionID, Package> {
    }
}
