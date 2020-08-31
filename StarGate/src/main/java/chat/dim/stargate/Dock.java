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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class Dock {

    // tasks for sending out
    private final List<Integer> priorityList = new ArrayList<>();
    private final Map<Integer, List<StarShip>> shipTables = new HashMap<>();
    private final ReentrantReadWriteLock shipLock = new ReentrantReadWriteLock();

    void addShip(StarShip task) {
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            int priority = task.priority;
            List<StarShip> table = shipTables.get(priority);
            if (table == null) {
                // create new table for this priority
                table = new ArrayList<>();
                shipTables.put(priority, table);
                // insert the priority in a sorted list
                int index = 0;
                for (; index < priorityList.size(); ++index) {
                    if (priority < priorityList.get(index)) {
                        // insert priority before the bigger one
                        break;
                    }
                }
                priorityList.add(index, priority);
            }
            // append to tail
            table.add(task);
        } finally {
            writeLock.unlock();
        }
    }

    StarShip getShip() {
        StarShip task = null;
        Lock writeLock = shipLock.writeLock();
        writeLock.lock();
        try {
            List<StarShip> table;
            for (int priority : priorityList) {
                table = shipTables.get(priority);
                if (table == null || table.size() == 0) {
                    continue;
                }
                // pop from the head
                task = table.remove(0);
                break;
            }
        } finally {
            writeLock.unlock();
        }
        return task;
    }
}
