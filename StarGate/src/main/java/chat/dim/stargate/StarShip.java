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

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Package;
import chat.dim.mtp.protocol.TransactionID;
import chat.dim.sg.Ship;
import chat.dim.tlv.Data;

public class StarShip implements Ship<Package, StarGate> {

    public static final int URGENT = -1;
    public static final int NORMAL = 0;
    public static final int SLOWER = 1;

    final int priority;

    private final Package pack;
    private final WeakReference<StarGate.Delegate> delegateRef;

    public StarShip(int priority, Package pack, StarGate.Delegate delegate) {
        super();
        this.priority = priority;
        this.pack = pack;
        delegateRef = new WeakReference<>(delegate);
    }

    public StarShip(int priority, byte[] payload, StarGate.Delegate delegate) {
        this(priority, Package.create(DataType.Message, payload.length, new Data(payload)), delegate);
    }

    TransactionID getTransactionID() {
        return pack.head.sn;
    }

    byte[] getRequestData() {
        return pack.getBytes();
    }

    //
    //  Ship
    //

    @Override
    public Package getPackage() {
        return pack;
    }

    @Override
    public StarGate.Delegate getDelegate() {
        return delegateRef.get();
    }
}
