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
package chat.dim.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import chat.dim.mtp.PackUtils;
import chat.dim.mtp.Package;
import chat.dim.mtp.StreamArrival;
import chat.dim.mtp.StreamDeparture;
import chat.dim.mtp.StreamDocker;
import chat.dim.mtp.TransactionID;
import chat.dim.net.BaseHub;
import chat.dim.port.Gate;
import chat.dim.stargate.TCPGate;
import chat.dim.tcp.ClientHub;

interface StarDelegate extends Gate.Delegate<StreamDeparture, StreamArrival, TransactionID> {
}

public final class StarTrek extends TCPGate<StreamDeparture, StreamArrival, TransactionID> {

    public final SocketAddress remoteAddress;

    public StarTrek(StarDelegate delegate, SocketAddress remote) {
        super(delegate);
        remoteAddress = remote;
    }

    @Override
    protected BaseHub createHub() {
        return new ClientHub(this);
    }

    @Override
    protected StreamDocker createDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        // TODO: check data format before create docker
        return new StreamDocker(remote, local, data, this);
    }

    public void start() throws IOException {
        connect(remoteAddress, null);
        (new Thread(this)).start();
    }

    void sendCommand(byte[] body, int priority) {
        Package pack = PackUtils.createCommand(body);
        Object worker = getDocker(remoteAddress, null, true);
        ((StreamDocker) worker).sendPackage(pack, priority);
    }

    void sendMessage(byte[] body, int priority) {
        Package pack = PackUtils.createMessage(body);
        Object worker = getDocker(remoteAddress, null, true);
        ((StreamDocker) worker).sendPackage(pack, priority);
    }

    public static StarTrek create(String host, int port, StarDelegate delegate) {
        SocketAddress remote = new InetSocketAddress(host, port);
        return new StarTrek(delegate, remote);
    }
}
