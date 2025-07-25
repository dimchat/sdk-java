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

    public final String filename;
    public final int line;

    public LogCaller(String filename, int line) {
        this.filename = filename;
        this.line = line;
    }

    @Override
    public String toString() {
        String name = filename.split("\\.")[0];
        return name + ":" + line;
    }

    //
    //  Factory
    //
    public static LogCaller trace(String tag) {
        String filename = null;
        int line = -1;
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        boolean flag = false;
        for (StackTraceElement element : traces) {
            filename = element.getFileName();
            if (filename != null && filename.endsWith(tag)) {
                flag = true;
            } else if (flag) {
                line = element.getLineNumber();
                break;
            }
        }
        assert filename != null && line >= 0 : "traces error: " + Arrays.toString(traces);
        return new LogCaller(filename, line);
    }

}
