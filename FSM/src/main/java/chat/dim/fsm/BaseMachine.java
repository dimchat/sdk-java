/* license: https://mit-license.org
 *
 *  Finite State Machine
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import java.lang.ref.WeakReference;

public abstract class BaseMachine<S extends State<Context>>
        implements Machine<Context, S, BaseTransition, Delegate<Context, S>> {

    private Status status = Status.Stopped;

    private WeakReference<Delegate<Context, S>> delegateRef = null;

    public Delegate<Context, S> getDelegate() {
        if (delegateRef == null) {
            return null;
        } else {
            return delegateRef.get();
        }
    }
    public void setDelegate(Delegate<Context, S> delegate) {
        if (delegate == null) {
            delegateRef = null;
        } else {
            delegateRef = new WeakReference<>(delegate);
        }
    }

    @Override
    public void changeState(S newState) {
        Context ctx = getContext();
        Delegate<Context, S> delegate = getDelegate();
        S oldState = getCurrentState();

        // events before state changed
        if (delegate != null) {
            if (oldState != null) {
                delegate.exitState(oldState, ctx);
            }
            if (newState != null) {
                delegate.enterState(newState, ctx);
            }
        }

        // change state
        setCurrentState(newState);

        // events after state changed
        if (oldState != null) {
            oldState.onExit(ctx);
        }
        if (newState != null) {
            newState.onEnter(ctx);
        }
    }

    /**
     *  start machine from default state
     */
    public void start() {
        changeState(getDefaultState());
        status = Status.Running;
    }

    /**
     *  stop machine and set current state to null
     */
    public void stop() {
        status = Status.Stopped;
        changeState(null);
    }

    /**
     *  pause machine, current state not change
     */
    public void pause() {
        Context ctx = getContext();
        S currentState = getCurrentState();
        Delegate<Context, S> delegate = getDelegate();
        if (delegate != null) {
            delegate.pauseState(currentState, ctx);
        }
        status = Status.Paused;
        currentState.onPause(ctx);
    }

    /**
     *  resume machine with current state
     */
    public void resume() {
        Context ctx = getContext();
        S currentState = getCurrentState();
        Delegate<Context, S> delegate = getDelegate();
        if (delegate != null) {
            delegate.resumeState(currentState, ctx);
        }
        status = Status.Running;
        currentState.onResume(ctx);
    }

    /**
     *  Drive the machine running forward
     */
    public void tick() {
        Context ctx = getContext();
        S currentState = getCurrentState();
        if (currentState != null && status == Status.Running) {
            currentState.tick(ctx);
        }
    }
}
