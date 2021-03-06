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

import chat.dim.mtp.DataType;
import chat.dim.mtp.Header;
import chat.dim.mtp.Package;
import chat.dim.startrek.Gate;
import chat.dim.startrek.Ship;
import chat.dim.startrek.StarDocker;
import chat.dim.startrek.StarShip;
import chat.dim.type.ByteArray;
import chat.dim.type.Data;

/**
 *  Star Docker for MTP packages
 */
public final class MTPDocker extends StarDocker {

    public static final int MAX_HEAD_LENGTH = 24;

    public MTPDocker(Gate gate) {
        super(gate);
    }

    public static Header parseHead(byte[] buffer) {
        Header head = Header.parse(new Data(buffer));
        if (head == null) {
            return null;
        }
        if (head.bodyLength < 0) {
            return null;
        }
        return head;
    }

    public static boolean check(Gate gate) {
        byte[] buffer = gate.receive(MAX_HEAD_LENGTH, false);
        if (buffer == null) {
            return false;
        } else {
            return parseHead(buffer) != null;
        }
    }

    @Override
    public StarShip pack(byte[] payload, int priority, Ship.Delegate delegate) {
        Data req = new Data(payload);
        Package mtp = Package.create(DataType.Message, req.getSize(), req);
        return new MTPShip(mtp, priority, delegate);
    }

    private Header seekHeader() {
        byte[] buffer = getGate().receive(512, false);
        if (buffer == null) {
            // received nothing
            return null;
        }
        Header head = parseHead(buffer);
        if (head == null) {
            // not a MTP package?
            if (buffer.length < MAX_HEAD_LENGTH) {
                // wait for more data
                return null;
            }
            // locate next header
            Data data = new Data(buffer);
            int pos = data.find(Header.MAGIC_CODE, 1);
            if (pos > 0) {
                // found next head(starts with 'DIM'), skip data before it
                getGate().receive(pos, true);
            } else if (buffer.length > 500) {
                // skip the whole buffer
                getGate().receive(buffer.length, true);
            }
        }
        return head;
    }

    private Package receivePackage() {
        // 1. seek header received data
        Header head = seekHeader();
        if (head == null) {
            // header not found
            return null;
        }
        int bodyLen = head.bodyLength;
        assert bodyLen >= 0 : "body length error: " + bodyLen;
        int packLen = head.getSize() + bodyLen;
        // 2. receive data with 'head.length + body.length'
        byte[] buffer = getGate().receive(packLen, false);
        if (buffer == null || buffer.length < packLen) {
            // waiting for more data
            return null;
        } else {
            // remove data from cache
            byte[] removed = getGate().receive(packLen, true);
            assert removed.length == packLen : "should not happen: " + buffer.length + ", " + removed.length;
        }
        // receive package
        Data data = new Data(buffer);
        ByteArray body = data.slice(head.getSize());
        return new Package(data, head, body);
    }

    @Override
    protected Ship getIncomeShip() {
        Package income = receivePackage();
        if (income == null) {
            return null;
        } else {
            return new MTPShip(income);
        }
    }

    @Override
    protected StarShip processIncomeShip(Ship income) {
        MTPShip ship = (MTPShip) income;
        Package mtp = ship.mtp;
        Header head = mtp.head;
        ByteArray body = mtp.body;
        // 1. check data type
        if (mtp.isCommand()) {
            // respond for Command directly
            if (body.equals(PING)) {        // 'PING'
                Data res = new Data(PONG);  // 'PONG'
                mtp = Package.create(DataType.CommandResponse, head.sn, PONG.length, res);
                return new MTPShip(mtp, StarShip.SLOWER);
            }
            return null;
        } else if (mtp.isCommandResponse()) {
            // just ignore
            return null;
        } else if (mtp.isMessageFragment()) {
            // should not happen
            return null;
        } else if (mtp.isMessageResponse()) {
            if (body.getSize() == 0 || body.equals(OK)) {
                // just ignore
                return null;
            } else if (body.equals(AGAIN)) {
                // TODO: mission failed, send the message again
                return null;
            }
        }
        // 2. process payload by delegate
        byte[] res = null;
        Gate.Delegate delegate = getGate().getDelegate();
        if (body.getSize() > 0 && delegate != null) {
            res = delegate.onReceived(getGate(), income);
        }
        // 3. response
        if (mtp.isMessage()) {
            // respond for message
            if (res == null || res.length == 0) {
                res = OK;
            }
            mtp = Package.create(DataType.MessageResponse, head.sn, res.length, new Data(res));
            return new MTPShip(mtp);
        } else if (res != null && res.length > 0) {
            // push as new Message
            return pack(res, StarShip.SLOWER, null);
        } else {
            return null;
        }
    }

    @Override
    protected void removeLinkedShip(Ship income) {
        MTPShip ship = (MTPShip) income;
        if (ship.mtp.isMessageResponse()) {
            super.removeLinkedShip(income);
        }
    }

    @Override
    protected StarShip getOutgoShip() {
        StarShip outgo = super.getOutgoShip();
        if (outgo instanceof MTPShip) {
            MTPShip ship = (MTPShip) outgo;
            // if retries == 0, means this ship is first time to be sent,
            // and it would be removed from the dock.
            if (outgo.getRetries() == 0 && ship.mtp.isMessage()) {
                // put back for waiting response
                getGate().parkShip(outgo);
            }
        }
        return outgo;
    }

    @Override
    protected StarShip getHeartbeat() {
        Package pack = Package.create(DataType.Command, PING.length, new Data(PING));
        return new MTPShip(pack, StarShip.SLOWER);
    }

    private static final byte[] PING = {'P', 'I', 'N', 'G'};
    private static final byte[] PONG = {'P', 'O', 'N', 'G'};
    private static final byte[] AGAIN = {'A', 'G', 'A', 'I', 'N'};
    private static final byte[] OK = {'O', 'K'};
}
