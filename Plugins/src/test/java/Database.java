
import java.util.List;

import chat.dim.core.Archivist;
import chat.dim.core.Barrack;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;


public class Database extends Barrack implements Archivist {

    @Override
    public void cacheUser(User user) {

    }

    @Override
    public void cacheGroup(Group group) {

    }

    @Override
    public User getUser(ID identifier) {
        return null;
    }

    @Override
    public Group getGroup(ID identifier) {
        return null;
    }

    //
    //  Archivist
    //

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // TODO:
        return false;
    }

    @Override
    public boolean saveDocument(Document doc) {
        // TODO:
        return false;
    }

    @Override
    public VerifyKey getMetaKey(ID user) {
        return null;
    }

    @Override
    public EncryptKey getVisaKey(ID user) {
        return null;
    }

    @Override
    public List<ID> getLocalUsers() {
        return null;
    }

}
