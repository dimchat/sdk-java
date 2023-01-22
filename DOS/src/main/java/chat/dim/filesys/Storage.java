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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Storage extends Resource implements Writable {

    //---- read

    @Override
    public boolean exists(String path) {
        return Paths.exists(path);
    }

    @Override
    public int read(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            // file not found
            throw new IOException("file not found: " + path);
        }
        try (InputStream is = new FileInputStream(file)) {
            return read(is);
        }
    }

    //---- write

    @Override
    public void setData(byte[] fileContent) {
        data = fileContent;
    }

    @Override
    public int write(String path) throws IOException {
        if (data == null) {
            return -1;
        }
        File file = new File(path);
        if (!file.exists()) {
            // check parent directory exists
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("failed to create directory: " + dir);
            }
        }
        try (OutputStream os = new FileOutputStream(file)) {
            return write(os);
        }
    }

    public int write(OutputStream os) throws IOException {
        os.write(data);
        return data.length;
    }

    @Override
    public boolean remove(String path) throws IOException {
        if (Paths.delete(path)) {
            return true;
        } else {
            throw new IOException("failed to remove: " + path);
        }
    }
}
