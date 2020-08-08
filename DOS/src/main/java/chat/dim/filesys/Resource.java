/* license: https://mit-license.org
 *
 *  File System
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
package chat.dim.filesys;

import java.io.IOException;
import java.io.InputStream;

public class Resource implements Readable {

    byte[] data = null;

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public boolean exists(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return is != null;
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public int read(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return -1;
            }
            return read(is);
        }
    }

    public int read(InputStream is) throws IOException {
        int size = is.available();
        data = new byte[size];
        int offset = 0;
        int length;
        while (offset < size) {
            length = is.read(data, offset, size - offset);
            if (length <= 0) {
                throw new IOException("failed to read data from input stream");
            }
            offset += length;
        }
        return offset;
    }
}
