/* license: https://mit-license.org
 *
 *  Finite State Machine
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
package chat.dim.fsm;

import java.util.HashMap;
import java.util.Map;

public class AutoMachine<S extends IState<S>> extends Machine<S> implements Runnable {

    private final Map<String, S> stateMap = new HashMap<>();

    private Thread thread = null;

    public AutoMachine(String defaultState) {
        super(defaultState);
    }

    public void addState(String name, S state) {
        stateMap.put(name, state);
    }

    @Override
    public S getState(String name) {
        if (name == null) {
            return null;
        } else {
            return stateMap.get(name);
        }
    }

    @Override
    public void start() {
        super.start();
        forceStop();
        thread = new Thread(this);
        thread.start();
    }

    private void forceStop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        thread = null;
    }

    @Override
    public void stop() {
        super.stop();
        forceStop();
    }

    @Override
    public void run() {
        setup();
        try {
            handle();
        } finally {
            finish();
        }
    }

    protected void setup() {
        // prepare for running
    }
    protected void finish() {
        // clean up after running
    }
    protected void handle() {
        while (getCurrentState() != null) {
            tick();
            idle();
        }
    }

    protected void idle() {
        try {
            Thread.sleep(128);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
