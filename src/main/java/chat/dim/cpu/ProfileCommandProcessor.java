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
package chat.dim.cpu;

import java.util.Locale;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;

public class ProfileCommandProcessor extends CommandProcessor {

    public ProfileCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content getProfile(ID identifier) {
        // query profile for ID
        Profile profile = getFacebook().getProfile(identifier);
        if (profile == null) {
            String text = String.format(Locale.CHINA, "Sorry, profile for %s not found", identifier);
            return new TextContent(text);
        } else {
            return new ProfileCommand(identifier, profile);
        }
    }

    private Content putProfile(ID identifier, Meta meta, Profile profile) {
        Facebook facebook = getFacebook();
        if (meta != null) {
            // received a meta for ID
            if (!facebook.saveMeta(meta, identifier)) {
                String text = String.format(Locale.CHINA, "Sorry, meta for %s error", identifier);
                return new TextContent(text);
            }
        }
        // receive a profile for ID
        if (facebook.saveProfile(profile))  {
            String text = String.format(Locale.CHINA, "Profile saved for %s", identifier);
            return new ReceiptCommand(text);
        } else {
            String text = String.format(Locale.CHINA, "Sorry, profile for %s error", identifier);
            return new TextContent(text);
        }
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof ProfileCommand;
        ProfileCommand cmd = (ProfileCommand) content;
        Profile profile = cmd.getProfile();
        ID identifier = getFacebook().getID(cmd.getIdentifier());
        if (profile == null) {
            return getProfile(identifier);
        } else {
            // check meta
            Meta meta;
            try {
                meta = cmd.getMeta();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            return putProfile(identifier, meta, profile);
        }
    }
}
