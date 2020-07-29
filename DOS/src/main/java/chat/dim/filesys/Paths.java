/* license: https://mit-license.org
 *
 *  File System
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
package chat.dim.filesys;

import java.io.File;

public abstract class Paths {

    public static String separator = File.separator;

    /**
     *  Append all components to the path with separator
     *
     * @param path       - root directory
     * @param components - sub-dir or filename
     * @return new path
     */
    public static String appendPathComponent(String path, String... components) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        for (String item: components) {
            sb.append(separator);
            sb.append(item);
        }
        return sb.toString();
    }

    /**
     *  Get filename from a URL
     *
     * @param url - url string
     * @return filename
     */
    public static String filenameFromURL(String url) {
        int pos = url.indexOf("?");
        if (pos >= 0) {
            url = url.substring(0, pos);
        }
        pos = url.indexOf("#");
        if (pos >= 0) {
            url = url.substring(0, pos);
        }
        return filename(url);
    }

    public static String filename(String path) {
        int pos = path.lastIndexOf(separator);
        if (pos >= 0) {
            return path.substring(pos + 1);
        }
        return path;
    }

    /**
     *  Get extension from a filename
     *
     * @param filename - file name
     * @return file extension
     */
    public static String extension(String filename) {
        int pos = filename.lastIndexOf(".");
        if (pos > 0) {
            return filename.substring(pos + 1);
        } else {
            return null;
        }
    }
}
