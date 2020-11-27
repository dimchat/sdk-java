
import org.junit.Test;

import chat.dim.User;
import chat.dim.Immortals;
import chat.dim.mkm.BroadcastAddress;
import chat.dim.protocol.ID;
import chat.dim.protocol.Profile;

public class ImmortalsTest {

    private Immortals immortals = new Immortals();
    private Facebook facebook = Facebook.getInstance();

    @Test
    public void testImmortals() {
        // Immortal Hulk
        User hulk = facebook.getUser(immortals.getID(Immortals.HULK));
        Log.info("hulk: " + hulk);

        Log.info("name: " + hulk.getName());
        Log.info("profile: " + facebook.getProfile(hulk.identifier, Profile.ANY));

        // Monkey King
        User moki = facebook.getUser(immortals.getID(Immortals.MOKI));
        Log.info("moki: " + moki);

        Log.info("name: " + moki.getName());
        Log.info("profile: " + facebook.getProfile(moki.identifier, Profile.ANY));

        // Everyone
        User anyone = new User(ID.ANYONE);
        anyone.setDataSource(facebook);
        Log.info("broadcast: " + anyone.identifier);
        Log.info("anyone: " + anyone);
        Log.info("is broadcast: " + (anyone.identifier.getAddress() instanceof BroadcastAddress));

        // Everyone
        User everyone = new User(ID.EVERYONE);
        everyone.setDataSource(facebook);
        Log.info("broadcast: " + everyone.identifier);
        Log.info("everyone: " + everyone);
        Log.info("is broadcast: " + (everyone.identifier.getAddress() instanceof BroadcastAddress));
    }
}