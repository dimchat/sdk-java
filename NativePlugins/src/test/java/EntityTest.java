
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.AsymmetricAlgorithms;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SignKey;
import chat.dim.mkm.BTCAddress;
import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Group;
import chat.dim.mkm.MetaUtils;
import chat.dim.mkm.User;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.Facebook;

public class EntityTest {

    private static final Facebook facebook = SharedFacebook.getInstance();
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
        info.put("did", doc.getIdentifier());
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

        address = BTCAddress.parse("4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        Log.info("address: " + address + ", detail: " + getAddressInfo(address));

        address = BTCAddress.parse("4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Log.info("address: " + address + ", detail: " + getAddressInfo(address));

        address = BTCAddress.parse(satoshi);
        Log.info("satoshi: " + address);
    }

    private String getAddressInfo(Address address) {
        // TODO:
        return null;
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

    @Test
    public void testMeta() {
        PrivateKey sk = PrivateKey.generate(AsymmetricAlgorithms.RSA);
        PublicKey pk = sk.getPublicKey();
        String seed = "moky";
        Meta meta = Meta.generate(MetaType.MKM, sk, seed);
        Log.info("meta: " + meta + ", detail: " + getMetaInfo(meta));
        Assert.assertTrue(MetaUtils.matches(pk, meta));

        ID identifier = ID.generate(meta, EntityType.USER.value, null);
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));
        Assert.assertTrue(MetaUtils.matches(identifier, meta));

        User user = new BaseUser(identifier);
        user.setDataSource(facebook);
//        facebook.cacheUser(user);
//
//        byte[] data = UTF8.encode(seed);
//
//        byte[] signature = user.sign(data);
//        Assert.assertTrue(user.verify(data, signature));
//        Log.info("signature OK!");
//
//        byte[] ciphertext = user.encrypt(data);
//        byte[] plaintext = user.decrypt(ciphertext);
//        Assert.assertArrayEquals(data, plaintext);
//        Log.info("decryption OK!");
    }

    @Test
    public void testProfile() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("did", "moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        dict.put("data", "{\"name\":\"齐天大圣\"}");
        dict.put("signature", "oMdD4Ssop/gOpzwAYpt+Cp3tVJswm+u5i1bu1UlEzzFt+g3ohmE1z018WmSgsBpCls6vXwJEhKS1O5gN9N8XCYhnYx/Q56M0n2NOSifcbQuZciOfQU1c2RMXgUEizIwL2tiFoam22qxyScKIjXcu7rD4XhBC0Gn/EhQpJCqWTMo=");
        Document doc = Document.parse(dict);
        Log.info("profile: " + doc);

        ID identifier = ID.parse("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        Meta meta = facebook.getMeta(identifier);
        if (meta != null && meta.isValid()) {
            doc.verify(meta.getPublicKey());
        }
        Log.info("profile: " + doc);

        doc.setProperty("age", 18);
        Log.info("profile: " + doc);

        SignKey key = facebook.getPrivateKeyForVisaSignature(identifier);
        if (key != null) {
            doc.sign(key);
        }
        Log.info("profile: " + doc);
    }

    @Test
    public void testGroup() {
        ID identifier = ID.parse("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        Group group = new BaseGroup(identifier);
        group.setDataSource(facebook);

//        Log.info("founder: " + group.getFounder());
//        Log.info("owner: " + group.getOwner());
//        Log.info("members: " + group.getMembers());
    }

}

