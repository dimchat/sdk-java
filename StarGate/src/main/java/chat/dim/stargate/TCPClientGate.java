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

import java.net.SocketAddress;
import java.util.List;

import chat.dim.mtp.MTPHelper;
import chat.dim.mtp.Package;
import chat.dim.mtp.StreamDocker;
import chat.dim.net.Connection;
import chat.dim.port.Docker;
import chat.dim.port.Ship;
import chat.dim.tcp.ClientHub;

public class TCPClientGate extends CommonGate<ClientHub> {

    public final SocketAddress remoteAddress;
    public final SocketAddress localAddress;

    public TCPClientGate(Docker.Delegate delegate, SocketAddress remote, SocketAddress local) {
        super(delegate);
        remoteAddress = remote;
        localAddress = local;
        setHub(createClientHub());
    }

    protected ClientHub createClientHub() {
        // override for user-customized hub
        return new ClientHub(this);
    }

    @Override
    protected Docker createDocker(List<byte[]> data,
                                  SocketAddress remote, SocketAddress local, Connection conn) {
        // TODO: check data format before create docker
        return new StreamDocker(remote, null, conn, getDelegate());
    }

    public boolean send(Package pack, int priority, Ship.Delegate delegate) {
        return send(localAddress, remoteAddress, pack, priority, delegate);
    }

    public boolean sendCommand(byte[] body, int priority, Ship.Delegate delegate) {
        Package pack = MTPHelper.createCommand(body);
        return send(pack, priority, delegate);
    }
    public boolean sendMessage(byte[] body, int priority, Ship.Delegate delegate) {
        Package pack = MTPHelper.createMessage(body);
        return send(pack, priority, delegate);
    }
}
