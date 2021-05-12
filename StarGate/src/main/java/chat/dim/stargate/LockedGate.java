/* license: https://mit-license.org
 *
 *  Star Gate: Network Connection Module
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
package chat.dim.stargate;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.tcp.Connection;

public class LockedGate extends TCPGate {

    private final ReadWriteLock sendLock = new ReentrantReadWriteLock();
    private final ReadWriteLock receiveLock = new ReentrantReadWriteLock();

    public LockedGate(Connection conn) {
        super(conn);
    }

    @Override
    public boolean send(byte[] pack) {
        boolean ok;
        Lock writeLock = sendLock.writeLock();
        writeLock.lock();
        try {
            ok = super.send(pack);
        } finally {
            writeLock.unlock();
        }
        return ok;
    }

    @Override
    public byte[] receive(int length, boolean remove) {
        byte[] data;
        Lock writeLock = receiveLock.writeLock();
        writeLock.lock();
        try {
            data = super.receive(length, remove);
        } finally {
            writeLock.unlock();
        }
        return data;
    }
}
