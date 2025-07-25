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

import java.util.Date;

import chat.dim.utils.TimeUtils;

public class DefaultLogger implements Logger {

    protected final LogPrinter printer;

    public DefaultLogger(LogPrinter logPrinter) {
        super();
        printer = logPrinter;
    }

    public DefaultLogger() {
        this(new LogPrinter());
    }

    protected String now() {
        if (Log.showTime) {
            Date now = TimeUtils.currentTime();
            return TimeUtils.getFullTimeString(now);
        } else {
            return null;
        }
    }

    protected LogCaller caller() {
        if (Log.showCaller) {
            return LogCaller.trace("Log.java");
        } else {
            return null;
        }
    }

    public static String shorten(String text, int maxLen) {
        assert maxLen > 128 : "too short: " + maxLen;
        int size = text.length();
        if (size <= maxLen) {
            return text;
        }
        String desc = "total " + size + " chars";
        int pos = (maxLen - desc.length() - 10) >> 1;
        if (pos <= 0) {
            return text;
        }
        String prefix = text.substring(0, pos);
        String suffix = text.substring(size - pos);
        return prefix + " ... " + desc + " ... " + suffix;
    }

    protected void output(String tag, String msg) {
        int maxLen = Log.MAX_LEN;
        if (maxLen > 0) {
            msg = shorten(msg, maxLen);
        }
        String head = tag + " | ";
        String body = msg;
        String tail = "";
        // insert time to head
        String time = now();
        if (time != null) {
            head = "[" + time + "] " + head;
        }
        // insert caller to body
        LogCaller locate = caller();
        if (locate != null) {
            body = locate + " >\t" + body;
        }
        printer.output(tag, locate, head, body, tail);
    }

    @Override
    public void debug(String msg) {
        int flag = Log.level & Log.DEBUG_FLAG;
        if (flag > 0) {
            output(Logger.DEBUG_TAG, msg);
        }
    }

    @Override
    public void info(String msg) {
        int flag = Log.level & Log.INFO_FLAG;
        if (flag > 0) {
            output(Logger.INFO_TAG, msg);
        }
    }

    @Override
    public void warning(String msg) {
        int flag = Log.level & Log.WARNING_FLAG;
        if (flag > 0) {
            output(Logger.WARNING_TAG, msg);
        }
    }

    @Override
    public void error(String msg) {
        int flag = Log.level & Log.ERROR_FLAG;
        if (flag > 0) {
            output(Logger.ERROR_TAG, msg);
        }
    }
}
