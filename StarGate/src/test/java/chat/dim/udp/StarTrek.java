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
package chat.dim.udp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import chat.dim.mtp.PackUtils;
import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDocker;
import chat.dim.port.Docker;
import chat.dim.port.Gate;
import chat.dim.stargate.UDPGate;

public final class StarTrek extends UDPGate<PackageHub> {

    public final SocketAddress remoteAddress;
    public final SocketAddress localAddress;

    public StarTrek(Gate.Delegate delegate, SocketAddress remote, SocketAddress local) {
        super(delegate);
        remoteAddress = remote;
        localAddress = local;
    }

    public static StarTrek create(String host, int port, Gate.Delegate delegate) {
        SocketAddress remote = new InetSocketAddress(host, port);
        StarTrek gate = new StarTrek(delegate, remote, null);
        gate.setHub(new PackageHub(gate));
        return gate;
    }

    @Override
    protected Docker createDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        // TODO: check data format before create docker
        return new PackageDocker(remote, local, this);
    }

    @Override
    public void start() {
        super.start();
        (new Thread(this)).start();
    }

    public void send(Package pack) {
        Docker worker = getDocker(remoteAddress, localAddress, null);
        ((PackageDocker) worker).send(pack);
    }

    public void sendCommand(byte[] body) {
        send(PackUtils.createCommand(body));
    }
    public void sendMessage(byte[] body) {
        send(PackUtils.createMessage(body));
    }
}