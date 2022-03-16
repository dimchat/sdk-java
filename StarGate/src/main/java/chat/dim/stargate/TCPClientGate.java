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
import chat.dim.mtp.PackageDeparture;
import chat.dim.mtp.StreamDocker;
import chat.dim.net.Connection;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.tcp.ClientHub;

public class TCPClientGate extends BaseGate<ClientHub> {

    public final SocketAddress remoteAddress;
    public final SocketAddress localAddress;

    public TCPClientGate(Docker.Delegate delegate, SocketAddress remote, SocketAddress local) {
        super(delegate);
        remoteAddress = remote;
        localAddress = local;
        setHub(createClientHub());
    }

    // override for user-customized hub
    protected ClientHub createClientHub() {
        return new ClientHub(this);
    }

    @Override
    protected Docker createDocker(Connection conn, List<byte[]> data) {
        // TODO: check data format before create docker
        StreamDocker docker = new StreamDocker(conn);
        docker.setDelegate(getDelegate());
        return docker;
    }

    //
    //  Sending
    //

    public boolean sendPackage(Package pack, int priority) {
        Docker docker = getDocker(remoteAddress, localAddress, null);
        if (docker == null || !docker.isOpen()) {
            return false;
        }
        Departure ship = new PackageDeparture(pack, priority);
        return docker.sendShip(ship);
    }

    public boolean sendCommand(byte[] body, int priority) {
        Package pack = MTPHelper.createCommand(body);
        return sendPackage(pack, priority);
    }
    public boolean sendMessage(byte[] body, int priority) {
        Package pack = MTPHelper.createMessage(body);
        return sendPackage(pack, priority);
    }
}
