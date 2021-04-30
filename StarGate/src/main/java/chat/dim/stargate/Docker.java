/* license: https://mit-license.org
 *
 *  Star Gate: Interfaces for network connection
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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

import java.lang.ref.WeakReference;
import java.util.Date;

import chat.dim.tcp.Connection;

public abstract class Docker implements Worker {

    private final WeakReference<StarGate> gateRef;

    private long heartbeatExpired;

    public Docker(StarGate gate) {
        super();
        gateRef = new WeakReference<>(gate);
        heartbeatExpired = (new Date()).getTime() + 2000;
    }

    public StarGate getGate() {
        return gateRef.get();
    }

    public Gate.Status getStatus() {
        return getGate().getStatus();
    }

    protected Connection getConnection() {
        return getGate().connection;
    }

    protected Dock getDock() {
        return getGate().dock;
    }

    public Gate.Delegate getDelegate() {
        return getGate().getDelegate();
    }

    protected boolean send(byte[] buffer) {
        return getConnection().send(buffer) == buffer.length;
    }

    protected byte[] received() {
        return getConnection().received();
    }

    protected byte[] receive(int length) {
        return getConnection().receive(length);
    }

    @Override
    public void setup() {
        // override to build the connection
    }

    @Override
    public void finish() {
        // TODO: go through all outgo Ships parking in Dock and call 'sent failed' on their delegates
    }

    @Override
    public boolean handle() {
        // 1. process income
        Ship income = getIncomeShip();
        if (income != null) {
            StarShip res = handleShip(income);
            if (res != null) {
                if (res.priority == StarShip.SLOWER) {
                    // put the response into waiting queue
                    getDock().put(res);
                } else {
                    send(res);
                }
            }
        }
        // 2. process outgo
        StarShip outgo = getOutgoShip();
        if (outgo != null) {
            if (outgo.isExpired()) {
                // outgo Ship expired, callback
                Ship.Delegate delegate = outgo.getDelegate();
                if (delegate != null) {
                    delegate.onSent(outgo, new Gate.Error(outgo, "Request timeout"));
                }
            } else if (!send(outgo)) {
                // failed to send outgo Ship, callback
                Ship.Delegate delegate = outgo.getDelegate();
                if (delegate != null) {
                    delegate.onSent(outgo, new Gate.Error(outgo, "Connection error"));
                }
            }
        }
        // 3. heartbeat
        if (income == null && outgo == null) {
            // check time for next heartbeat
            long now = (new Date()).getTime();
            if (now > heartbeatExpired) {
                StarShip beat = getHeartbeat();
                if (beat != null) {
                    // put the heartbeat into waiting queue
                    getDock().put(beat);
                }
                // try heartbeat next 2 seconds
                heartbeatExpired = now + 2000;
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     *  Get income Ship from Connection
     */
    protected abstract Ship getIncomeShip();

    // Override to process income SHip
    protected StarShip handleShip(Ship income) {
        StarShip linked = getOutgoShip(income);
        if (linked != null) {
            Ship.Delegate delegate = linked.getDelegate();
            if (delegate != null) {
                delegate.onSent(linked, null);
            }
        }
        return null;
    }

    /**
     *  Get outgo Ship from waiting queue
     */
    protected StarShip getOutgoShip() {
        // get next new task (time == 0)
        StarShip outgo = getDock().pop();
        if (outgo == null) {
            // no more new task now, get any expired task
            outgo = getDock().any();
        }
        return outgo;
    }

    /**
     *  get task with ID (income.SN)
     */
    protected StarShip getOutgoShip(Ship income) {
        return getDock().pop(income.getSN());
    }

    /**
     *  Send outgo Ship via current Connection
     */
    protected abstract boolean send(StarShip outgo);

    /**
     *  Get an empty ship for keeping connection alive
     */
    protected StarShip getHeartbeat() {
        return null;
    }
}
