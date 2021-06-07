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

/**
 *  State Machine Delegate
 *  ~~~~~~~~~~~~~~~~~~~~~~
 */
public interface Delegate<S extends State> {

    /**
     * Callback when entering a new state
     *
     * @param state   - new state
     * @param machine - state machine
     */
    void enterState(S state, Machine<S> machine);

    /**
     * Callback when exit from current state
     *
     * @param state   - old state
     * @param machine - state machine
     */
    void exitState(S state, Machine<S> machine);

    /**
     * Callback when pause current state
     *
     * @param state   - current state
     * @param machine - state machine
     */
    void pauseState(S state, Machine<S> machine);

    /**
     * Callback when resume current state
     *
     * @param state   - current state
     * @param machine - state machine
     */
    void resumeState(S state, Machine<S> machine);
}
