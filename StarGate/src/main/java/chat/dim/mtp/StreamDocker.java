/* license: https://mit-license.org
 *
 *  MTP: Message Transfer Protocol
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
package chat.dim.mtp;

import java.net.SocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Ship;
import chat.dim.startrek.StarGate;
import chat.dim.stream.SeekerResult;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public class StreamDocker extends PackageDocker {

    public StreamDocker(SocketAddress remote, SocketAddress local, StarGate gate) {
        super(remote, local, gate);
    }

    private ByteArray chunks = Data.ZERO;
    private boolean received = false;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    protected Package parsePackage(final byte[] data) {
        Package pack;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            // join the data to the memory cache
            ByteArray buffer = chunks.concat(data);
            chunks = Data.ZERO;
            // try to fetch a package
            SeekerResult<Package> result = MTPHelper.seekPackage(buffer);
            pack = result.value;
            received = pack != null;
            int offset = result.offset;
            if (offset >= 0) {
                // 'error part' + 'MTP package' + 'remaining data'
                if (pack != null) {
                    offset += pack.getSize();
                }
                if (offset == 0) {
                    chunks = buffer.concat(chunks);
                } else if (offset < buffer.getSize()) {
                    chunks = buffer.slice(offset).concat(chunks);
                }
            }
        } finally {
            writeLock.unlock();
        }
        return pack;
    }

    @Override
    public void processReceived(byte[] data) {
        // the cached data maybe contain sticky packages,
        // so we need to process them circularly here
        received = true;
        while (received) {
            received = false;
            super.processReceived(data);
            data = new byte[0];
        }
    }

    @Override
    protected Arrival createArrival(final Package pkg) {
        return new StreamArrival(pkg);
    }

    @Override
    protected Departure createDeparture(Package pkg, int priority, Ship.Delegate delegate) {
        return new StreamDeparture(pkg, priority, delegate);
    }

    @Override
    protected void respondCommand(TransactionID sn, byte[] body) {
        send(MTPHelper.respondCommand(sn, body));
    }

    @Override
    protected void respondMessage(TransactionID sn, int pages, int index) {
        send(MTPHelper.respondMessage(sn, pages, index, OK));
    }

    @Override
    public Departure pack(byte[] payload, int priority, Ship.Delegate delegate) {
        Package pkg = MTPHelper.createMessage(payload);
        return createDeparture(pkg, priority, delegate);
    }

    @Override
    public void heartbeat() {
        Package pkg = MTPHelper.createCommand(PING);
        appendDeparture(createDeparture(pkg, Departure.Priority.SLOWER.value, null));
    }
}
