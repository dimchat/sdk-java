/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
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
package chat.dim.protocol;

import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.dkd.Envelope;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command : "receipt",
 *      message : "...",
 *      // -- extra info
 *      sender    : "...",
 *      receiver  : "...",
 *      time      : 0,
 *      signature : "..." // the same signature with the original message
 *  }
 */
public class ReceiptCommand extends Command {

    // original message info
    private Envelope envelope = null;
    private byte[] signature = null;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public ReceiptCommand(String message, Envelope envelope, byte[] signature) {
        super(RECEIPT);
        setMessage(message);
        setEnvelope(envelope);
        setSignature(signature);
    }

    public ReceiptCommand(String message, Envelope envelope) {
        this(message, envelope, null);
    }

    public ReceiptCommand(String message) {
        this(message, null, null);
    }

    //-------- setters/getters --------

    public String getMessage() {
        return (String) dictionary.get("message");
    }

    public void setMessage(String message) {
        if (message != null) {
            dictionary.put("message", message);
        }
    }

    public Envelope getEnvelope() {
        if (envelope == null) {
            // envelope: { sender: "...", receiver: "...", time: 0 }
            Object env = dictionary.get("envelope");
            if (env == null) {
                Object sender = dictionary.get("sender");
                Object receiver = dictionary.get("receiver");
                if (sender != null && receiver != null) {
                    env = dictionary;
                }
            }
            envelope = Envelope.getInstance(env);
        }
        return envelope;
    }

    public void setEnvelope(Envelope env) {
        if (env != null) {
            dictionary.put("sender", env.sender);
            dictionary.put("receiver", env.receiver);
            dictionary.put("time", env.time);
            // message type
            ContentType type = env.getType();
            if (type != null) {
                dictionary.put("type", type.value);
            }
            // group ID
            Object group = env.getGroup();
            if (group != null) {
                dictionary.put("group", group);
            }
        }
        envelope = env;
    }

    public byte[] getSignature() {
        if (signature == null) {
            String base64 = (String) dictionary.get("signature");
            if (base64 == null) {
                signature = null;
            } else {
                signature = Base64.decode(base64);
            }
        }
        return signature;
    }

    public void setSignature(byte[] sig) {
        if (sig != null) {
            dictionary.put("signature", Base64.encode(sig));
        }
        signature = sig;
    }
}
