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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.net.Connection;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.stream.SeekerResult;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public class StreamDocker extends PackageDocker {

    public StreamDocker(Connection conn) {
        super(conn);
    }

    private final ReadWriteLock chunksLock = new ReentrantReadWriteLock();
    private ByteArray chunks = Data.ZERO;
    private boolean packageReceived = false;

    @Override
    protected Package parsePackage(byte[] data) {
        Package pack;
        Lock writeLock = chunksLock.writeLock();
        writeLock.lock();
        try {
            // join the data to the memory cache
            ByteArray buffer = chunks.concat(data);
            chunks = Data.ZERO;
            // try to fetch a package
            SeekerResult<Package> result = MTPHelper.seekPackage(buffer);
            pack = result.value;
            packageReceived = pack != null;
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
        packageReceived = true;
        while (packageReceived) {
            packageReceived = false;
            super.processReceived(data);
            data = new byte[0];
        }
    }

    @Override
    protected Arrival checkArrival(Arrival income) {
        StreamArrival ship = (StreamArrival) income;
        Package pack = ship.getPackage();
        if (pack == null) {
            List<Package> fragments = ship.getFragments();
            pack = fragments.get(fragments.size() - 1);
        }
        // check body length
        if (pack.head.bodyLength != pack.body.getSize()) {
            // sticky data?
            return ship;
        }
        // check for response
        return super.checkArrival(income);
    }

    @Override
    protected Arrival createArrival(Package pkg) {
        return new StreamArrival(pkg);
    }

    @Override
    protected Departure createDeparture(Package pkg, int priority) {
        return new StreamDeparture(pkg, priority, 0);
    }

    //
    //  Packing
    //

    @Override
    protected Package createCommand(byte[] body) {
        return MTPHelper.createCommand(body);
    }

    @Override
    protected Package createMessage(byte[] body) {
        return MTPHelper.createMessage(body);
    }

    @Override
    protected Package createCommandResponse(TransactionID sn, byte[] body) {
        return MTPHelper.respondCommand(sn, body);
    }

    @Override
    protected Package createMessageResponse(TransactionID sn, int pages, int index) {
        return MTPHelper.respondMessage(sn, pages, index, OK);
    }
}
