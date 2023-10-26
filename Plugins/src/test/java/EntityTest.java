
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.Group;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class EntityTest {

    private static final Facebook facebook = Facebook.getInstance();
    private static final String satoshi = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";

    private String getIDInfo(ID identifier) {
        Map<String, Object> info = new HashMap<>();
        info.put("type", identifier.getType());
        info.put("name", identifier.getName());
        info.put("address", identifier.getAddress());
        info.put("terminal", identifier.getTerminal());
        return info.toString();
    }

    private String getMetaInfo(Meta meta) {
        Map<String, Object> info = new HashMap<>();
        info.put("version", meta.getType());
        info.put("key", meta.getPublicKey());
        info.put("seed", meta.getSeed());
        info.put("fingerprint", meta.getFingerprint());
        return info.toString();
    }

    private String getDocumentInfo(Document doc) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", doc.getIdentifier());
        info.put("name", doc.getName());
        info.put("key", doc.getProperty("key"));
        info.put("avatar", doc.getProperty("avatar"));
        info.put("properties", doc.getProperties());
        info.put("valid", doc.isValid());
        return info.toString();
    }

    @Test
    public void testAddress() {
        Address address;

//        address = BTCAddress.parse("4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
//        Log.info("address: " + address + ", detail: " + getAddressInfo(address));
//
//        address = BTCAddress.parse("4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
//        Log.info("address: " + address + ", detail: " + getAddressInfo(address));

//        address = BTCAddress.parse(satoshi);
//        Log.info("satoshi: " + address);
    }

    @Test
    public void testID() {
        ID identifier;

        identifier = ID.parse("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));

        Log.info("is broadcast: " + identifier.isBroadcast());

        //Assert.assertEquals(identifier, ID.parse("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ/home"));

        List<ID> array = new ArrayList<>();
        array.add(identifier);
        array.add(identifier);
        Log.info("list<ID>: " + array);
    }

//    @Test
//    public void testMeta() throws ClassNotFoundException {
//        PrivateKey sk = PrivateKey.generate(PrivateKey.RSA);
//        PublicKey pk = sk.getPublicKey();
//        String seed = "moky";
//        byte[] data = UTF8.encode(seed);
//        Meta meta = Meta.generate(MetaType.Default, sk, seed);
//        Log.info("meta: " + meta + ", detail: " + getMetaInfo(meta));
//        Assert.assertTrue(meta.matches(pk));
//
//        ID identifier = meta.generateID(NetworkID.Main);
//        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));
//        Assert.assertTrue(meta.matches(identifier));
//
//        User user = new User(identifier);
//        user.setDataSource(facebook);
//        facebook.cache(user);
//
//        byte[] signature = user.sign(data);
//        Assert.assertTrue(user.verify(data, signature));
//        Log.info("signature OK!");
//
//        byte[] ciphertext = user.encrypt(data);
//        byte[] plaintext = user.decrypt(ciphertext);
//        Assert.assertArrayEquals(data, plaintext);
//        Log.info("decryption OK!");
//    }

    @Test
    public void testGroup() {
        ID identifier = ID.parse("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        Group group = new BaseGroup(identifier);
        group.setDataSource(facebook);

        Log.info("founder: " + group.getFounder());
        Log.info("owner: " + group.getOwner());
        Log.info("members: " + group.getMembers());
    }
}

