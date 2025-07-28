/* license: https://mit-license.org
 *
 *  LNC: Log, Notification & Cache
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
package chat.dim.log;

public final class Log {

    public static final int DEBUG_FLAG   = 1;
    public static final int INFO_FLAG    = 1 << 1;
    public static final int WARNING_FLAG = 1 << 2;
    public static final int ERROR_FLAG   = 1 << 3;

    public static final int DEBUG   = DEBUG_FLAG | INFO_FLAG | WARNING_FLAG | ERROR_FLAG;
    public static final int DEVELOP =              INFO_FLAG | WARNING_FLAG | ERROR_FLAG;
    public static final int RELEASE =                          WARNING_FLAG | ERROR_FLAG;

    public static int MAX_LEN = 1024;

    public static int level = RELEASE;

    public static boolean showTime = false;
    public static boolean showCaller = true;
    public static boolean showMethod = false;

    public static Logger logger = new DefaultLogger();

    public static void debug(String msg) {
        logger.debug(msg);
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warning(String msg) {
        logger.warning(msg);
    }

    public static void error(String msg) {
        logger.error(msg);
    }
}
