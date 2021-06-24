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
import java.util.HashMap;
import java.util.Map;

public abstract class BaseMachine<C extends Context,
        T extends BaseTransition<C>,
        S extends State<C, T>,
        D extends Delegate<C, T, S>> implements Machine<C, T, S, D>, Context {

    private Status status = Status.Stopped;

    private WeakReference<D> delegateRef = null;

    private final Map<String, S> stateMap = new HashMap<>();
    private final String defaultStateName;
    private S currentState;

    public BaseMachine(String defaultState) {
        super();
        defaultStateName = defaultState;
    }

    //
    //  States
    //
    public void addState(String name, S state) {
        stateMap.put(name, state);
    }
    @Override
    public S getDefaultState() {
        return stateMap.get(defaultStateName);
    }
    @Override
    public S getTargetState(T transition) {
        return stateMap.get(transition.target);
    }
    @Override
    public S getCurrentState() {
        return currentState;
    }
    @Override
    public void setCurrentState(S currentState) {
        this.currentState = currentState;
    }

    public D getDelegate() {
        if (delegateRef == null) {
            return null;
        } else {
            return delegateRef.get();
        }
    }
    public void setDelegate(D delegate) {
        if (delegate == null) {
            delegateRef = null;
        } else {
            delegateRef = new WeakReference<>(delegate);
        }
    }

    @Override
    public void changeState(S newState) {
        C ctx = getContext();
        S oldState = getCurrentState();
        D delegate = getDelegate();

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
        C ctx = getContext();
        S currentState = getCurrentState();
        D delegate = getDelegate();
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
        C ctx = getContext();
        S currentState = getCurrentState();
        D delegate = getDelegate();
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
        C ctx = getContext();
        S state = getCurrentState();
        if (state != null && status == Status.Running) {
            T transition = state.evaluate(ctx);
            if (transition != null) {
                state = getTargetState(transition);
                assert state != null : "state error: " + transition;
                changeState(state);
            }
        }
    }
}
