/* license: https://mit-license.org
 *
 *  Star Gate: Interfaces for network connection
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

import chat.dim.mtp.protocol.DataType;
import chat.dim.mtp.protocol.Header;
import chat.dim.mtp.protocol.Package;
import chat.dim.tcp.Connection;
import chat.dim.tlv.Data;

/**
 *  Docker for MTP packages
 */
public class MTPDocker extends Docker {

    public MTPDocker(StarGate gate) {
        super(gate);
    }

    public static boolean check(Connection connection) {
        byte[] buffer = connection.received();
        if (buffer == null) {
            return false;
        } else {
            return Header.parse(new Data(buffer)) != null;
        }
    }

    @Override
    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        Data req = new Data(payload);
        Package pack = Package.create(DataType.Message, req.getLength(), req);
        StarShip ship = new MTPShip(pack, priority, delegate);
        return getDock().put(ship);
    }

    private Package receivePackage() {
        // 1. check received data
        byte[] buffer = received();
        if (buffer == null) {
            // received nothing
            return null;
        }
        Data data = new Data(buffer);
        Header head = Header.parse(data);
        if (head == null) {
            // not a MTP package?
            if (buffer.length < 20) {
                // wait for more data
                return null;
            }
            int pos = data.find(Header.MAGIC_CODE, 1);
            if (pos > 0) {
                // found next head(starts with 'DIM'), skip data before it
                receive(pos);
            } else {
                // skip thw whole data
                receive(buffer.length);
            }
            return null;
        }
        // 2. receive data with 'head.length + body.length'
        int bodyLen = head.bodyLength;
        if (bodyLen < 0) {
            // should not happen
            bodyLen = buffer.length - head.getLength();
        }
        int packLen = head.getLength() + bodyLen;
        if (packLen > buffer.length) {
            // waiting for more data
            return null;
        }
        // receive package
        buffer = receive(packLen);
        data = new Data(buffer);
        Data body = data.slice(head.getLength());
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
    protected StarShip handleShip(Ship income) {
        MTPShip ship = (MTPShip) income;
        Package pack = ship.getPackage();
        Header head = pack.head;
        Data body = pack.body;
        DataType type = head.type;
        // 1. check data type
        if (type.equals(DataType.Command)) {
            // respond for Command directly
            if (body.equals(PING)) {
                Data res = new Data(PONG);
                pack = Package.create(DataType.CommandRespond, head.sn, PONG.length, res);
                return new MTPShip(pack, StarShip.SLOWER);
            }
            return null;
        } else if (type.equals(DataType.CommandRespond)) {
            // remove linked outgo Ship
            return super.handleShip(income);
        } else if (type.equals(DataType.MessageFragment)) {
            // just ignore
            return null;
        } else if (type.equals(DataType.MessageRespond)) {
            // remove linked outgo Ship
            super.handleShip(income);
            if (body.getLength() == 0 || body.equals(OK)) {
                // just ignore
                return null;
            } else if (body.equals(AGAIN)) {
                // TODO: mission failed, send the message again
                return null;
            }
        }
        // 2. process payload by delegate
        byte[] res = null;
        Gate.Delegate delegate = getDelegate();
        if (body.getLength() > 0 && delegate != null) {
            res = delegate.onReceived(getGate(), body.getBytes());
        }
        // 3. response
        if (type.equals(DataType.Message)) {
            // respond for message
            if (res == null || res.length == 0) {
                res = OK;
            }
            pack = Package.create(DataType.MessageRespond, head.sn, res.length, new Data(res));
            return new MTPShip(pack, StarShip.NORMAL);
        } else if (res != null && res.length > 0) {
            // push as new Message
            send(res, StarShip.SLOWER, null);
        }
        return null;
    }

    @Override
    protected boolean send(StarShip outgo) {
        MTPShip ship = (MTPShip) outgo;
        Package pack = ship.getPackage();
        // check data type
        if (pack.head.type.equals(DataType.Message)) {
            // put back for response
            getDock().put(outgo);
        }
        // send out request data
        return send(pack.getBytes());
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
