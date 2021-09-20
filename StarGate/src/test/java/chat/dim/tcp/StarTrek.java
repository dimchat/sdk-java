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
package chat.dim.tcp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import chat.dim.mtp.PackUtils;
import chat.dim.mtp.Package;
import chat.dim.port.Gate;
import chat.dim.stargate.TCPGate;

public final class StarTrek extends TCPGate<ClientHub> {

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
        gate.setHub(new ClientHub(gate));
        return gate;
    }

    @Override
    public void start() {
        super.start();
        (new Thread(this)).start();
    }

    public void sendCommand(byte[] body) {
        Package pack = PackUtils.createCommand(body);
        send(pack, localAddress, remoteAddress);
    }
    public void sendMessage(byte[] body) {
        Package pack = PackUtils.createMessage(body);
        send(pack, remoteAddress, localAddress);
    }
}
