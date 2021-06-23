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

import java.util.ArrayList;
import java.util.List;

public abstract class State<S extends IState<S>> implements IState<S> {

    private final List<ITransition<S>> transitionList = new ArrayList<>();

    public void addTransition(ITransition<S> transition) {
        if (transitionList.contains(transition)) {
            throw new ArithmeticException("transition exists");
        }
        transitionList.add(transition);
    }

    @Override
    public void tick(IMachine<S> machine) {
        for (ITransition<S> transition : transitionList) {
            if (transition.evaluate(machine)) {
                // OK, get target state
                S state = transition.getTargetState(machine);
                if (state == null) {
                    throw new NullPointerException("failed to get target state: " + transition);
                }
                machine.changeState(state);
                break;
            }
        }
    }
}
