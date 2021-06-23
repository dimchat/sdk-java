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

public abstract class Machine<S extends IState<S>> implements IMachine<S> {

    private enum Status {
        Stopped (0),
        Running (1),
        Paused  (2);

        final int value;

        Status(int value) {
            this.value = value;
        }
    }
    private Status status = Status.Stopped;

    private WeakReference<Delegate<IMachine<S>, S>> delegateRef = null;

    private S currentState = null;
    private final String defaultStateName;

    public Machine(String defaultStateName) {
        super();
        this.defaultStateName = defaultStateName;
    }

    public Delegate<IMachine<S>, S> getDelegate() {
        if (delegateRef == null) {
            return null;
        } else {
            return delegateRef.get();
        }
    }
    public void setDelegate(Delegate<IMachine<S>, S> delegate) {
        if (delegate == null) {
            delegateRef = null;
        } else {
            delegateRef = new WeakReference<>(delegate);
        }
    }

    @Override
    public S getCurrentState() {
        return currentState;
    }

    @Override
    public void changeState(S newState) {
        Delegate<IMachine<S>, S> delegate = getDelegate();
        S oldState = currentState;

        // events before state changed
        if (delegate != null) {
            if (oldState != null) {
                delegate.exitState(oldState, this);
            }
            if (newState != null) {
                delegate.enterState(newState, this);
            }
        }

        // change state
        currentState = newState;

        // events after state changed
        if (oldState != null) {
            oldState.onExit(this);
        }
        if (newState != null) {
            newState.onEnter(this);
        }
    }

    /**
     *  start machine from default state
     */
    public void start() {
        assert currentState == null && Status.Stopped.equals(status) :
                "FSM start error: " + status + ", " + currentState;
        S defaultState = getState(defaultStateName);
        assert defaultState != null : "FSM default state not found: " + defaultStateName;
        changeState(defaultState);
        status = Status.Running;
    }

    /**
     *  stop machine and set current state to null
     */
    public void stop() {
        assert currentState != null && !Status.Stopped.equals(status) :
                "FSM stop error: " + status + ", " + currentState;
        status = Status.Stopped;
        changeState(null);
    }

    /**
     *  pause machine, current state not change
     */
    public void pause() {
        assert currentState != null && Status.Running.equals(status) :
                "FSM pause error: " + status + ", " + currentState;
        Delegate<IMachine<S>, S> delegate = getDelegate();
        if (delegate != null) {
            delegate.pauseState(currentState, this);
        }
        status = Status.Paused;
        currentState.onPause(this);
    }

    /**
     *  resume machine with current state
     */
    public void resume() {
        assert currentState != null && Status.Paused.equals(status) :
                "FSM resume error: " + status + ", " + currentState;
        Delegate<IMachine<S>, S> delegate = getDelegate();
        if (delegate != null) {
            delegate.resumeState(currentState, this);
        }
        status = Status.Running;
        currentState.onResume(this);
    }

    /**
     *  Drive the machine running forward
     */
    public void tick() {
        if (currentState != null && status == Status.Running) {
            currentState.tick(this);
        }
    }
}
