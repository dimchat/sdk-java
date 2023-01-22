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
import java.util.ArrayList;
import java.util.List;

import chat.dim.utils.ArrayUtils;

public interface Paths {

    /**
     *  Append all components to the path with separator
     *
     * @param path       - root directory
     * @param components - sub-dir or filename
     * @return new path
     */
    static String append(String path, String... components) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        for (String item: components) {
            sb.append(File.separator);
            sb.append(item);
        }
        return sb.toString();
    }

    /**
     *  Get filename from a URL/Path
     *
     * @param path - uri string
     * @return filename
     */
    static String filename(String path) {
        int pos;
        // ignore URI query string
        pos = path.indexOf("?");
        if (pos >= 0) {
            path = path.substring(0, pos);
        }
        // ignore URI fragment
        pos = path.indexOf("#");
        if (pos >= 0) {
            path = path.substring(0, pos);
        }
        // get last path component
        pos = path.lastIndexOf("/");
        if (pos < 0) {
            pos = path.lastIndexOf("\\");
            if (pos < 0) {
                // only filename
                return path;
            }
        }
        // cut parent
        return path.substring(pos + 1);
    }

    /**
     *  Get extension from a filename
     *
     * @param filename - file name
     * @return file extension
     */
    static String extension(String filename) {
        int pos = filename.lastIndexOf(".");
        if (pos < 0) {
            // no extension
            return null;
        }
        return filename.substring(pos + 1);
    }

    /**
     *  Get parent directory
     *
     * @param path - full path
     * @return parent path
     */
    static String parent(String path) {
        int pos;
        if (path.endsWith("/")) {
            pos = path.lastIndexOf("/", path.length() - 2);
        } else if (path.endsWith("\\")) {
            pos = path.lastIndexOf("\\", path.length() - 2);
        } else {
            pos = path.lastIndexOf("/");
            if (pos < 0) {
                pos = path.lastIndexOf("\\");
            }
        }
        if (pos < 0) {
            // relative path?
            return null;
        } else if (pos == 0) {
            // root dir: "/"
            return "/";
        }
        return path.substring(0, pos);
    }

    /**
     *  Get absolute path
     *
     * @param relative - relative path
     * @param base     - base directory
     * @return absolute path
     */
    static String abs(String relative, String base) {
        assert base.length() > 0 && relative.length() > 0 : "paths error: " + base + ", " + relative;
        if (relative.startsWith("/") || relative.indexOf(":") > 0) {
            // Linux   - "/filename"
            // Windows - "C:\\filename"
            // URL     - "file://filename"
            return relative;
        }
        String path;
        if (base.endsWith("/") || base.endsWith("\\")) {
            path = base + relative;
        } else {
            String separator = base.contains("\\") ? "\\" : "/";
            path = base + separator + relative;
        }
        if (path.contains("./")) {
            return tidy(path, "/");
        } else if (path.contains(".\\")) {
            return tidy(path, "\\");
        } else {
            return path;
        }
    }

    /**
     *  Remove relative components in full path
     *
     * @param path      - full path
     * @param separator - file separator
     * @return absolute path
     */
    static String tidy(String path, final String separator) {
        final String parent = ".." + separator;
        final String current = "." + separator;
        List<String> array = new ArrayList<>();
        String next;
        int left, right = 0;
        while (right >= 0) {
            left = right;
            right = path.indexOf(separator, left);
            if (right < 0) {
                // last component
                next = path.substring(left);
            } else {
                // next component (ends with the separator)
                right += separator.length();
                next = path.substring(left, right);
            }
            if (next.equals(parent)) {
                // backward
                assert array.size() > 0 : "path error: " + path;
                array.remove(array.size() - 1);
            } else if (!next.equals(current)) {
                array.add(next);
            }
        }
        return ArrayUtils.join("", array);
    }

    //
    //  Read
    //

    /**
     *  Check whether file exists
     *
     * @param path - file path
     * @return true on exists
     */
    static boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    //
    //  Write
    //

    /**
     *  Create directory
     *
     * @param path - dir path
     * @return false on error
     */
    static boolean mkdirs(String path) {
        File file = new File(path);
        if (file.exists()) {
            // already exists
            return file.isDirectory();
        } else {
            return file.mkdirs();
        }
    }

    /**
     *  Delete file
     *
     * @param path - file path
     * @return false on error
     */
    static boolean delete(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.delete();
        } else {
            // not exists
            return true;
        }
    }
}
