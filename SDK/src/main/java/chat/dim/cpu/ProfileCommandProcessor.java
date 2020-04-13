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

import chat.dim.*;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;

public class ProfileCommandProcessor extends CommandProcessor {

    public ProfileCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content getProfile(ID identifier) {
        Facebook facebook = getFacebook();
        // query profile for ID
        Profile profile = facebook.getProfile(identifier);
        if (profile == null || !profile.containsKey("data")) {
            // profile not found
            String text = String.format("Sorry, profile not found for ID: %s", identifier);
            return new TextContent(text);
        }
        // response
        return new ProfileCommand(identifier, profile);
    }

    private Content putProfile(ID identifier, Meta meta, Profile profile) {
        Facebook facebook = getFacebook();
        if (meta != null) {
            // received a meta for ID
            if (!facebook.verify(meta, identifier)) {
                // meta not match
                String text = String.format("Meta not match ID: %s", identifier);
                return new TextContent(text);
            }
            if (!facebook.saveMeta(meta, identifier)) {
                // meta not match
                String text = String.format("Meta not accept: %s", identifier);
                return new TextContent(text);
            }
        }
        // receive a profile for ID
        if (!facebook.verify(profile, identifier)) {
            // profile signature not match
            String text = String.format("Profile not match ID: %s", identifier);
            return new TextContent(text);
        }
        if (!facebook.saveProfile(profile))  {
            // save profile failed
            String text = String.format("Profile not accept: %s", identifier);
            return new TextContent(text);
        }
        // response
        String text = String.format("Profile received: %s", identifier);
        return new ReceiptCommand(text);
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage rMsg) {
        assert content instanceof ProfileCommand : "profile command error: " + content;
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
