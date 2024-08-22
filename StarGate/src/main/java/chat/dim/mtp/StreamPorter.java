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
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.pack.DeparturePacker;
import chat.dim.pack.SeekerResult;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

/**
 *  Docker for MTP packages
 */
public class StreamPorter extends PackagePorter implements DeparturePacker {

    private ByteArray chunks = Data.ZERO;
    private final ReadWriteLock chunksLock = new ReentrantReadWriteLock();
    private boolean packageReceived = false;

    public StreamPorter(SocketAddress remote, SocketAddress local) {
        super(remote, local);
    }

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
            int offset = result.offset;
            packageReceived = pack != null;
            if (offset >= 0) {
                // 'error part' + 'MTP package' + 'remaining data'
                if (pack != null) {
                    offset += pack.getSize();
                }
                if (offset == 0) {
                    chunks = buffer.concat(chunks);
                } else if (offset < buffer.getSize()) {
                    buffer = buffer.slice(offset);
                    chunks = buffer.concat(chunks);
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
        assert income instanceof StreamArrival : "arrival ship error: " + income;
        StreamArrival ship = (StreamArrival) income;
        Package pack = ship.getPackage();
        if (pack == null) {
            List<Package> fragments = ship.getFragments();
            int count = fragments.size();
            assert count > 0 : "fragments empty: " + ship;
            pack = fragments.get(count - 1);
        }
        Header head = pack.head;
        // check body length
        if (head.bodyLength != pack.body.getSize()) {
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
        if (pkg.isMessage()) {
            // normal package
            return new StreamDeparture(pkg, priority);
        } else {
            // response package needs no response again,
            // so this ship will be removed immediately after sent.
            return new StreamDeparture(pkg, priority, 1);
        }
    }

    //
    //  Packing
    //


    @Override
    protected Package createCommand(byte[] body) {
        return MTPHelper.createCommand(new Data(body));
    }

    @Override
    protected Package createMessage(byte[] body) {
        return MTPHelper.createMessage(null, new Data(body));
    }

    @Override
    protected Package createCommandResponse(TransactionID sn, byte[] body) {
        return MTPHelper.respondCommand(sn, new Data(body));
    }

    @Override
    protected Package createMessageResponse(TransactionID sn, int pages, int index) {
        return MTPHelper.respondMessage(sn, pages, index, new Data(OK));
    }

    @Override
    public Departure packData(byte[] payload, int priority) {
        Package pack = MTPHelper.createMessage(null, new Data(payload));
        return createDeparture(pack, priority);
    }

    public static boolean check(ByteArray data) {
        SeekerResult<Header> result = MTPHelper.seekHeader(data);
        return result.value != null;
    }

}
