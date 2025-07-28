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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DefaultLogger implements Logger {

    protected final LogPrinter printer;

    public DefaultLogger(LogPrinter logPrinter) {
        super();
        assert logPrinter != null : "log printer not found";
        printer = logPrinter;
    }

    public DefaultLogger() {
        this(new LogPrinter());
    }

    protected String now() {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return formatter.format(now);
    }

    protected LogCaller caller() {
        // Override for customized caller tracer
        return new LogCaller("Log.java", Thread.currentThread().getStackTrace());
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

    protected void output(String msg, String tag) {
        //
        //  1. shorten message
        //
        int maxLen = Log.MAX_LEN;
        if (maxLen > 0) {
            msg = shorten(msg, maxLen);
        }
        //
        //  2. build body
        //
        String body;
        //  2.1. insert caller & method
        LogCaller locate = caller();
        String method = null;
        if (Log.showMethod) {
            method = locate.getMethodName();
        }
        if (Log.showCaller) {
            if (method == null) {
                body = locate + " >\t" + msg;
            } else {
                body = locate + " | " + method + " >\t" + msg;
            }
        } else {
            if (method == null) {
                body = msg;
            } else {
                body = method + " >\t" + msg;
            }
        }
        //  2.2. insert time
        if (Log.showTime) {
            body = "[" + now() + "] " + tag + " | " + body;
        } else {
            body = tag + " | " + body;
        }
        //
        //  3. print
        //
        printer.output("", body, "", tag, locate);
    }

    @Override
    public void debug(String msg) {
        int flag = Log.level & Log.DEBUG_FLAG;
        if (flag > 0) {
            output(msg, Logger.DEBUG_TAG);
        }
    }

    @Override
    public void info(String msg) {
        int flag = Log.level & Log.INFO_FLAG;
        if (flag > 0) {
            output(msg, Logger.INFO_TAG);
        }
    }

    @Override
    public void warning(String msg) {
        int flag = Log.level & Log.WARNING_FLAG;
        if (flag > 0) {
            output(msg, Logger.WARNING_TAG);
        }
    }

    @Override
    public void error(String msg) {
        int flag = Log.level & Log.ERROR_FLAG;
        if (flag > 0) {
            output(msg, Logger.ERROR_TAG);
        }
    }
}
