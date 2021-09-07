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

import java.io.IOException;
import java.net.SocketAddress;

import chat.dim.net.BaseHub;
import chat.dim.net.Connection;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.startrek.StarGate;

public abstract class TCPGate<D extends Departure<A, I>, A extends Arrival<A, I>, I>
        extends StarGate<D, A, I> {

    private final BaseHub hub;

    public TCPGate(Delegate<D, A, I> delegate) {
        super(delegate);
        hub = createHub();
    }

    protected abstract BaseHub createHub();

    @Override
    public boolean process() {
        hub.tick();
        boolean available = hub.getActivatedCount() > 0;
        boolean busy = super.process();
        return available || busy;
    }

    @Override
    protected Connection getConnection(SocketAddress remote, SocketAddress local) {
        return hub.getConnection(remote, local);
    }

    @Override
    protected Connection connect(SocketAddress remote, SocketAddress local) throws IOException {
        return hub.connect(remote, local);
    }
}
