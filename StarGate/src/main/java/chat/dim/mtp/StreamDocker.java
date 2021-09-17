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

import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Ship;
import chat.dim.startrek.StarGate;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public class StreamDocker extends PackageDocker {

    public StreamDocker(SocketAddress remote, SocketAddress local, StarGate gate) {
        super(remote, local, gate);
    }

    private ByteArray chunks = Data.ZERO;
    private int processing = 0;

    @Override
    protected Package parsePackage(final byte[] data) {
        ++processing;
        if (processing > 1) {
            // it's already in processing now,
            // append the data to the tail of memory cache
            if (data != null && data.length > 0) {
                chunks = chunks.concat(data);
            }
            --processing;
            return null;
        }
        // append the data to the memory cache
        ByteArray buffer;
        if (data != null && data.length > 0) {
            buffer = chunks.concat(data);
        } else {
            buffer = chunks;
        }
        chunks = Data.ZERO;
        // check header
        final Header head = PackUtils.parseHead(buffer);
        if (head == null) {
            // header error, seeking for next header
            int pos = buffer.find(Header.MAGIC_CODE, 1);
            if (pos > 0) {
                // found, drop all data before it
                buffer = buffer.slice(pos);
                if (buffer.getSize() > 0) {
                    // join to the memory cache
                    if (chunks.getSize() > 0) {
                        chunks = buffer.concat(chunks);
                    } else {
                        chunks = buffer;
                    }
                }
                if (chunks.getSize() > 0) {
                    // try again
                    --processing;
                    return parsePackage(null);
                }
            }
            // waiting for more data
            --processing;
            return null;
        }
        // header ok, check body length
        int dataLen = buffer.getSize();
        int headLen = head.getSize();
        int bodyLen = head.bodyLength;
        int packLen = bodyLen == -1 ? dataLen : headLen + bodyLen;
        //assert bodyLen != -1;
        if (dataLen < packLen) {
            // waiting for more data
            --processing;
            return null;
        }
        if (dataLen > packLen) {
            // cut the tail and put it back to the memory cache
            if (chunks.getSize() > 0) {
                chunks = buffer.slice(packLen).concat(chunks);
            } else {
                chunks = buffer.slice(packLen);
            }
            buffer = buffer.slice(0, packLen);
        }
        // OK
        --processing;
        return new Package(buffer, head, buffer.slice(headLen));
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
        send(PackUtils.respondCommand(sn, body));
    }

    @Override
    protected void respondMessage(TransactionID sn, int pages, int index) {
        send(PackUtils.respondMessage(sn, pages, index, OK));
    }

    @Override
    public Departure pack(byte[] payload, int priority, Ship.Delegate delegate) {
        Package pkg = PackUtils.createMessage(payload);
        return createDeparture(pkg, priority, delegate);
    }

    @Override
    public void heartbeat() {
        Package pkg = PackUtils.createCommand(PING);
        appendDeparture(createDeparture(pkg, Departure.Priority.SLOWER.value, null));
    }
}
