
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.*;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SignKey;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;

public class EntityTest {

    static Facebook facebook = Facebook.getInstance();
    static String satoshi = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";

    private String getAddressInfo(Address address) {
        Map<String, Object> info = new HashMap<>();
        info.put("type", address.getCode());
        info.put("number", address.getNetwork());
        return info.toString();
    }

    private String getIDInfo(ID identifier) {
        Map<String, Object> info = new HashMap<>();
        info.put("type", identifier.getType());
        info.put("number", identifier.getNumber());
        info.put("valid", identifier.isValid());
        info.put("name", identifier.name);
        info.put("address", identifier.address);
        info.put("terminal", identifier.terminal);
        return info.toString();
    }

    private String getMetaInfo(Meta meta) {
        Map<String, Object> info = new HashMap<>();
        info.put("version", meta.getVersion());
        info.put("key", meta.getKey());
        info.put("seed", meta.getSeed());
        info.put("fingerprint", meta.getFingerprint());
        return info.toString();
    }

    private String getProfileInfo(Profile profile) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", profile.getIdentifier());
        info.put("name", profile.getName());
        info.put("key", profile.getKey());
        info.put("avatar", profile.getProperty("avatar"));
        info.put("properties", profile.propertyNames());
        info.put("valid", profile.isValid());
        return info.toString();
    }

    @Test
    public void testAddress() {
        Address address;

        address = Address.getInstance("4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        Log.info("address: " + address + ", detail: " + getAddressInfo(address));

        address = Address.getInstance("4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Log.info("address: " + address + ", detail: " + getAddressInfo(address));

        NetworkType robot = NetworkType.Robot;
        Log.info("robot type: " + robot.toByte());
        Log.info("robot type: " + robot.value);
        Assert.assertEquals((byte) 0xC8, robot.toByte());

        address = Address.getInstance(satoshi);
        Log.info("satoshi: " + address);
    }

    @Test
    public void testID() {
        ID identifier;

        identifier = ID.getInstance(Immortals.MOKI);
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));
        Assert.assertEquals(1840839527L, identifier.getNumber());

        identifier = ID.getInstance("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));
        Assert.assertEquals(4049699527L, identifier.getNumber());

        Log.info("is broadcast: " + identifier.isBroadcast());

        Assert.assertEquals(identifier, ID.getInstance("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ/home"));

        List<ID> array = new ArrayList<>();
        array.add(identifier);
        array.add(identifier);
        Log.info("list<ID>: " + array);

        Assert.assertTrue(identifier.isValid());
    }

    @Test
    public void testMeta() throws ClassNotFoundException {
        PrivateKey sk = PrivateKey.generate(PrivateKey.RSA);
        PublicKey pk = sk.getPublicKey();
        String seed = "moky";
        byte[] data = seed.getBytes(Charset.forName("UTF-8"));
        Meta meta = Meta.generate(MetaType.Default, sk, seed);
        Log.info("meta: " + meta + ", detail: " + getMetaInfo(meta));
        Assert.assertTrue(meta.matches(pk));

        ID identifier = meta.generateID(NetworkType.Main);
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));
        Assert.assertTrue(meta.matches(identifier));
        Assert.assertTrue(meta.matches(identifier.address));

        Assert.assertTrue(identifier.getType().isUser());
        Assert.assertTrue(identifier.getType().isPerson());

        Facebook facebook = Facebook.getInstance();
        facebook.cache(sk, identifier);
        facebook.cache(meta, identifier);

        User user = new User(identifier);
        user.setDataSource(facebook);
        facebook.cache(user);

        byte[] signature = user.sign(data);
        Assert.assertTrue(user.verify(data, signature));
        Log.info("signature OK!");

        byte[] ciphertext = user.encrypt(data);
        byte[] plaintext = user.decrypt(ciphertext);
        Assert.assertArrayEquals(data, plaintext);
        Log.info("decryption OK!");
    }

    @Test
    public void testProfile() {
        Map<String, Object> dict = new HashMap<>();
        dict.put("ID", "moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        dict.put("data", "{\"name\":\"齐天大圣\"}");
        dict.put("signature", "oMdD4Ssop/gOpzwAYpt+Cp3tVJswm+u5i1bu1UlEzzFt+g3ohmE1z018WmSgsBpCls6vXwJEhKS1O5gN9N8XCYhnYx/Q56M0n2NOSifcbQuZciOfQU1c2RMXgUEizIwL2tiFoam22qxyScKIjXcu7rD4XhBC0Gn/EhQpJCqWTMo=");
        Profile profile = Profile.getInstance(dict);
        Log.info("profile: " + profile);

        ID identifier = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        Meta meta = facebook.getMeta(identifier);
        if (meta.isValid()) {
            profile.verify(meta.getKey());
        }
        Log.info("profile: " + profile);

        profile.setProperty("age", 18);
        Log.info("profile: " + profile);

        SignKey key = facebook.getPrivateKeyForSignature(identifier);
        profile.sign(key);
        Log.info("profile: " + profile);
    }

    @Test
    public void testEntity() {
        ID identifier = ID.getInstance("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        Log.info("ID: " + identifier + ", detail: " + getIDInfo(identifier));

        User account = facebook.getUser(identifier);
        Log.info("account: " + account);
        Assert.assertEquals(4049699527L, account.getNumber());

        identifier = ID.getInstance(Immortals.MOKI);
        User user = facebook.getUser(identifier);
        Log.info("user: " + user);
        Assert.assertEquals(1840839527L, user.getNumber());

        if (account.equals(user)) {
            Log.info("same entity");
        }
        Assert.assertEquals(account.getType(), user.getType());

        Profile profile = user.getProfile();
        if (profile != null) {
            Log.info("profile: " + getProfileInfo(profile));
        }

        List<ID> contacts = user.getContacts();
        Log.info("contacts: " + contacts);
    }

    @Test
    public void testGroup() {
        ID identifier = ID.getInstance("Group-1280719982@7oMeWadRw4qat2sL4mTdcQSDAqZSo7LH5G");
        Group group = new Group(identifier);
        group.setDataSource(facebook);

        Log.info("founder: " + group.getFounder());
        Log.info("owner: " + group.getOwner());
        Log.info("members: " + group.getMembers());
    }
}

