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

import chat.dim.mtp.MTPHelper;
import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDeparture;
import chat.dim.net.Hub;
import chat.dim.port.Departure;
import chat.dim.port.Docker;

public abstract class CommonGate<H extends Hub>
        extends AutoGate<H> {

    public CommonGate(Docker.Delegate delegate, boolean isDaemon) {
        super(delegate, isDaemon);
    }

    //
    //  Docker
    //

    @Override
    public Docker getDocker(SocketAddress remote, SocketAddress local) {
        return super.getDocker(remote, null);
    }

    @Override
    protected void setDocker(SocketAddress remote, SocketAddress local, Docker docker) {
        super.setDocker(remote, null, docker);
    }

    @Override
    protected void removeDocker(SocketAddress remote, SocketAddress local, Docker docker) {
        super.removeDocker(remote, null, docker);
    }

    //
    //  Sending
    //

    public boolean send(SocketAddress source, SocketAddress destination,
                        Departure ship) {
        Docker worker = getDocker(destination, source, null);
        return worker != null && worker.appendDeparture(ship);
    }

    public boolean send(SocketAddress source, SocketAddress destination, Package pack, int priority) {
        Departure ship = new PackageDeparture(pack, priority);
        return send(source, destination, ship);
    }

    public boolean send(SocketAddress source, SocketAddress destination, byte[] payload, int priority) {
        Package pack = MTPHelper.createMessage(payload);
        return send(source, destination, pack, priority);
    }

    public boolean send(SocketAddress source, SocketAddress destination, byte[] payload) {
        return send(source, destination, payload, NORMAL);
    }
    public boolean send(SocketAddress source, SocketAddress destination, Package pack) {
        return send(source, destination, pack, NORMAL);
    }
    static final int NORMAL = Departure.Priority.NORMAL.value;
}
