/* license: https://mit-license.org
 *
 *  LNC: Log, Notification & Cache
 *
 *                                Written in 2025 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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
package chat.dim.log;

import java.util.Arrays;

public class LogCaller {

    private final String tag;                  // anchor tag
    private final StackTraceElement[] stacks;  // stack traces
    private StackTraceElement caller;

    public LogCaller(String anchor, StackTraceElement[] traces) {
        super();
        tag = anchor;
        stacks = traces;
    }

    @Override
    public String toString() {
        String name = getClassName();
        if (name == null) {
            name = getFilename();
            if (name != null) {
                name = name.split("\\.")[0];
            }
        }
        return name + ":" + getLineNumber();
    }

    public String getFilename() {
        StackTraceElement element = getCaller();
        return element == null ? null : element.getFileName();
    }

    public int getLineNumber() {
        StackTraceElement element = getCaller();
        return element == null ? -1 : element.getLineNumber();
    }

    public String getClassName() {
        StackTraceElement element = getCaller();
        return element == null ? null : element.getClassName();
    }

    public String getMethodName() {
        StackTraceElement element = getCaller();
        return element == null ? null : element.getMethodName();
    }

    //
    //  trace caller
    //

    private StackTraceElement getCaller() {
        StackTraceElement element = caller;
        if (element == null) {
            element = locate(tag, stacks);
            caller = element;
        }
        return element;
    }

    /**
     *  Locate the real caller: next element of the anchor(s)
     */
    protected StackTraceElement locate(String anchor, StackTraceElement[] traces) {
        boolean flag = false;
        for (StackTraceElement element : traces) {
            if (checkAnchor(anchor, element)) {
                // skip anchor(s)
                flag = true;
            } else if (flag) {
                // get next element of the anchor(s)
                return element;
            }
        }
        assert false : "caller not found: " + anchor + " -> " + Arrays.toString(traces);
        return null;
    }

    protected boolean checkAnchor(String anchor, StackTraceElement element) {
        String filename = element.getFileName();
        return anchor.equals(filename);
    }

}
