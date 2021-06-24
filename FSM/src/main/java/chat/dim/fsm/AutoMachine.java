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

public abstract class AutoMachine<C extends Context, T extends BaseTransition<C>, S extends State<C, T>>
        extends BaseMachine<C, T, S, Delegate<C, T, S>> implements Context, Runnable {

    private Thread thread = null;

    public AutoMachine(String defaultState) {
        super(defaultState);
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
    protected void handle() {
        while (getCurrentState() != null) {
            tick();
            idle();
        }
    }
    protected void finish() {
        // clean up after running
    }

    protected void idle() {
        try {
            Thread.sleep(128);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
