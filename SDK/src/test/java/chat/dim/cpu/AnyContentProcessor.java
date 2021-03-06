
package chat.dim.cpu;

import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Content;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;

public class AnyContentProcessor extends ContentProcessor {

    public AnyContentProcessor() {
        super();
    }

    protected Content unknown(Content content, ReliableMessage rMsg) {
        String text;

        // File: Image, Audio, Video
        if (content instanceof FileContent) {
            if (content instanceof ImageContent) {
                // Image
                text = "Image received";
            } else if (content instanceof AudioContent) {
                // Audio
                text = "Voice message received";
            } else if (content instanceof VideoContent) {
                // Video
                text = "Movie received";
            } else {
                // other file
                text = "File received";
            }
        } else if (content instanceof TextContent) {
            // Text
            text = "Text message received";
        } else if (content instanceof PageContent) {
            // Web page
            text = "Web page received";
        } else {
            text = "Content (type: " + content.getType() + ") not support yet!";
            TextContent res = new TextContent(text);
            ID group = content.getGroup();
            if (group != null) {
                res.setGroup(group);
            }
            return res;
        }

        Object group = content.getGroup();
        if (group != null) {
            // DON'T response group message for disturb reason
            return null;
        }

        // response
        Object signature = rMsg.get("signature");
        ReceiptCommand receipt = new ReceiptCommand(text, content.getSerialNumber(), rMsg.getEnvelope());
        receipt.put("signature", signature);
        return receipt;
    }
}
