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

import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDeparture;
import chat.dim.mtp.StreamDocker;
import chat.dim.net.Hub;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.startrek.DepartureShip;

public class TCPGate<H extends Hub> extends CommonGate<H> {

    public TCPGate(Delegate delegate) {
        super(delegate);
    }

    @Override
    protected Docker createDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        // TODO: check data format before create docker
        return new StreamDocker(remote, local, this) {
            @Override
            protected Departure getNextDeparture(final long now) {
                Departure outgo = super.getNextDeparture(now);
                if (outgo == null) {
                    return null;
                }
                if (outgo.getRetries() >= DepartureShip.MAX_RETRIES) {
                    // last try
                    return outgo;
                }
                if (outgo instanceof PackageDeparture) {
                    Package pack = ((PackageDeparture) outgo).getPackage();
                    if (!pack.isResponse()) {
                        // put back for next retry
                        appendDeparture(outgo);
                    }
                }
                return outgo;
            }
        };
    }

    public void send(Package pack, SocketAddress source, SocketAddress destination,
                     int priority, Delegate delegate) {
        Docker worker = getDocker(destination, source, null);
        ((StreamDocker) worker).send(pack, priority, delegate);
    }

    public void send(Package pack, SocketAddress source, SocketAddress destination) {
        final int priority = Departure.Priority.NORMAL.value;
        send(pack, source, destination, priority, getDelegate());
    }
}
