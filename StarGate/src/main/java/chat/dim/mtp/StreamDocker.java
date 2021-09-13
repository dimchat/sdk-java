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

import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.util.List;

import chat.dim.net.Connection;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Ship;
import chat.dim.startrek.DepartureShip;
import chat.dim.startrek.StarDocker;
import chat.dim.startrek.StarGate;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

public class StreamDocker extends StarDocker {

    private final WeakReference<StarGate> gateRef;

    public StreamDocker(SocketAddress remote, SocketAddress local, StarGate gate) {
        super(remote, local);
        gateRef = new WeakReference<>(gate);
    }

    @Override
    protected Connection getConnection() {
        StarGate gate = gateRef.get();
        if (gate == null) {
            return null;
        }
        return gate.getConnection(getRemoteAddress(), getLocalAddress());
    }

    @Override
    protected Ship.Delegate getDelegate() {
        StarGate gate = gateRef.get();
        if (gate == null) {
            return null;
        }
        return gate.getDelegate();
    }

    private ByteArray chunks = Data.ZERO;

    private Package parse(final byte[] data) {
        if (data != null && data.length > 0) {
            // append to tail
            chunks = chunks.concat(data);
        }
        // check header
        final Header head = PackUtils.parseHead(chunks);
        if (head == null) {
            // header error, seeking for next header
            int pos = chunks.find(Header.MAGIC_CODE, 1);
            if (pos > 0) {
                // found, drop all data before it
                chunks = chunks.slice(pos);
                // try again
                return parse(null);
            } else {
                // not found, drop all data
                chunks = Data.ZERO;
                return null;
            }
        }
        // header ok, check body length
        int dataLen = chunks.getSize();
        int headLen = head.getSize();
        int bodyLen = head.bodyLength;
        int packLen = bodyLen == -1 ? dataLen : headLen + bodyLen;
        //assert bodyLen != -1;
        if (dataLen < packLen) {
            // waiting for more data
            return null;
        }
        ByteArray pack = chunks.slice(0, packLen);
        chunks = chunks.slice(packLen);
        return new Package(pack, head, pack.slice(headLen));
    }

    @Override
    protected Arrival getArrival(final byte[] data) {
        final Package pack = parse(data);
        if (pack == null) {
            return null;
        }
        final ByteArray body = pack.body;
        if (body == null || body.getSize() == 0) {
            // should not happen
            return null;
        }
        return new StreamArrival(pack);
    }

    @Override
    protected Arrival checkArrival(final Arrival income) {
        assert income instanceof StreamArrival : "arrival ship error: " + income;
        StreamArrival ship = (StreamArrival) income;
        Package pack = ship.getPackage();
        if (pack == null) {
            List<Package> fragments = ship.getFragments();
            if (fragments == null || fragments.size() == 0) {
                throw new NullPointerException("fragments error: " + income);
            }
            // each ship can carry one fragment only
            pack = fragments.get(0);
        }
        // check data type in package header
        final Header head = pack.head;
        final DataType type = head.type;
        final ByteArray body = pack.body;

        if (type.isCommandResponse()) {
            // process CommandResponse:
            //      'PONG'
            //      'OK'
            checkResponse(income);
            if (body.equals(PONG) || body.equals(OK)) {
                // command responded
                return null;
            }
            // extra data in CommandResponse?
            // let the caller to process it
        } else if (type.isCommand()) {
            // process Command:
            //      'PING'
            //      '...'
            if (body.equals(PING)) {
                // PING -> PONG
                send(PackUtils.respondCommand(head.sn, PONG), Departure.Priority.SLOWER);
                return null;
            }
            // respond for Command
            send(PackUtils.respondCommand(head.sn, OK));
            // Unknown Command?
            // let the caller to process it
        } else if (type.isMessageResponse()) {
            // process MessageResponse:
            //      'OK'
            //      'AGAIN'
            if (body.equals(AGAIN)) {
                // TODO: reset maxRetries?
                return null;
            }
            checkResponse(income);
            if (body.equals(OK)) {
                // message responded
                return null;
            }
            // extra data in MessageResponse?
            // let the caller to process it
        } else if (type.isMessageFragment()) {
            // assemble MessageFragment with cached fragments to completed Message
            // let the caller to process the completed message
            return assembleArrival(income);
        } else if (type.isMessage()) {
            // respond for Message
            send(PackUtils.respondMessage(head.sn, head.pages, head.index, OK));
            // let the caller to process the message
        }

        if (body.getSize() == 4) {
            if (body.equals(NOOP)) {
                // do nothing
                return null;
            } else if (body.equals(PING) || body.equals(PONG)) {
                // FIXME: these bodies should be in a Command
                // ignore them
                return null;
            }
        }

        return income;
    }

    @Override
    protected Departure getNextDeparture(final long now) {
        Departure outgo = super.getNextDeparture(now);
        if (outgo != null && outgo.getRetries() < DepartureShip.MAX_RETRIES) {
            // put back for next retry
            appendDeparture(outgo);
        }
        return outgo;
    }

    public void send(Package pack) {
        send(pack, Departure.Priority.NORMAL.value);
    }
    public void send(Package pack, Departure.Priority priority) {
        send(pack, priority.value);
    }
    public void send(Package pack, int priority) {
        appendDeparture(new StreamDeparture(priority, pack));
    }

    @Override
    public Departure pack(byte[] payload, int priority) {
        Package pack = PackUtils.createMessage(payload);
        return new StreamDeparture(priority, pack);
    }

    @Override
    public void heartbeat() {
        Package pack = PackUtils.createCommand(PING);
        send(pack, Departure.Priority.SLOWER.value);
    }

    static final byte[] PING = {'P', 'I', 'N', 'G'};
    static final byte[] PONG = {'P', 'O', 'N', 'G'};
    static final byte[] NOOP = {'N', 'O', 'O', 'P'};
    static final byte[] OK = {'O', 'K'};
    static final byte[] AGAIN = {'A', 'G', 'A', 'I', 'N'};
}
