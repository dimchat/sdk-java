/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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
package chat.dim.format;

import java.net.URI;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.type.Dictionary;

public class BaseNetworkFile extends Dictionary implements PortableNetworkFile {

    private URI remoteURL;                // download from CDN
    private TransportableData attachment; // file content (not encrypted)
    private DecryptKey password;          // key to decrypt data

    public BaseNetworkFile(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy load
        remoteURL = null;
        attachment = null;
        password = null;
    }

    //
    //  Create PNF with remote URL & decrypt key
    //

    public BaseNetworkFile(URI url, DecryptKey key) {
        super();
        setURL(url);
        attachment = null;
        setPassword(key);
    }

    //
    //  Create PNF with file data & filename
    //

    public BaseNetworkFile(byte[] data, String filename) {
        super();
        remoteURL = null;
        setData(data);
        setFilename(filename);
        password = null;
    }

    @Override
    public void setURL(URI url) {
        if (url == null) {
            remove("URL");
        } else {
            put("URL", url.toString());
        }
        remoteURL = url;
    }

    @Override
    public URI getURL() {
        URI url = remoteURL;
        if (url == null) {
            String remote = getString("URL", null);
            if (remote != null) {
                remoteURL = url = URI.create(remote);
            }
        }
        return url;
    }

    @Override
    public void setData(byte[] data) {
        TransportableData ted;
        if (data != null/* && data.length > 0*/) {
            ted = TransportableData.create(data);
            // lazy encode
            // put("data", ted.toObject());
        } else {
            ted = null;
            remove("data");
        }
        attachment = ted;
    }

    @Override
    public byte[] getData() {
        TransportableData ted = attachment;
        if (ted == null) {
            Object base64 = get("data");
            attachment = ted = TransportableData.parse(base64);
        }
        return ted == null ? null : ted.getData();
    }

    @Override
    public void setFilename(String filename) {
        if (filename == null) {
            remove("filename");
        } else {
            put("filename", filename);
        }
    }

    @Override
    public String getFilename() {
        return getString("filename", null);
    }

    @Override
    public void setPassword(DecryptKey key) {
        setMap("key", key);
        password = key;
    }

    @Override
    public DecryptKey getPassword() {
        if (password == null) {
            Object info = get("key");
            password = SymmetricKey.parse(info);
        }
        return password;
    }

    private Object encodeData() {
        Object base64 = get("data");
        if (base64 == null) {
            // 'data' not exists, check attachment
            TransportableData ted = attachment;
            if (ted != null) {
                // encode data string
                base64 = ted.toObject();
                put("data", base64);
            }
        }
        // return encoded data string
        return base64;
    }

    @Override
    public String toString() {
        // check 'data' and 'key'
        if (encodeData() != null || getPassword() != null) {
            // not a single URL, encode the entire dictionary
            return JSONMap.encode(toMap());
        }
        // field 'data' not exists, means this file was uploaded onto a CDN,
        // if 'key' not exists too, just return 'URL' string here.
        assert get("filename") == null : "PNF error: " + toMap();
        String url = getString("URL", null);
        assert url != null : "URL cannot be empty: " + toMap();
        return url;
    }

    @Override
    public Object toObject() {
        return toString();
    }
}
