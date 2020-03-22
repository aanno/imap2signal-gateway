package com.github.aanno.imap2signal;

import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.asamk.Signal;
import org.asamk.signal.manager.Manager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.security.Security;
import java.time.Instant;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * https://stackoverflow.com/questions/28689099/javamail-reading-recent-unread-mails-using-imap
 */
public class MailFetch implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MailFetch.class);

    private static final int MAX_ENTRIES = 40;

    private static final String KEYRING_COLLECTION_NAME = "imap2signal";
    private static final String KEYRING_COLLECTION_SECRET = "secret";
    private static final String PREFERENCES_LAST_LOOKUP = "timeMillisOfLastCheck";

    private static final String SIGNAL_CONFIG_DIR = ".local/share/signal-cli";

    private static final String MAIL_ACCOUNT = "aannoaanno@gmail.com";
    private static final String SIGNAL_ACCOUNT = "sender";
    private static final String SIGNAL_RECIPIENTS = "recipient";

    public static void main(String[] args) throws Exception, EncapsulatedExceptions {
        String mailAccount = MAIL_ACCOUNT;
        boolean testOnly = false;
        if (args.length > 0) {
            mailAccount = args[0];
        }
        if ("test".equals(mailAccount)) {
            testOnly = true;
        }
        LOG.info("mail account: " + mailAccount + " testOnly: " + testOnly);
        long now = System.currentTimeMillis();
        try (MailFetch dut = new MailFetch(testOnly)) {
            if (testOnly) {
                // only for tests
                dut.setLastCheck(Long.MIN_VALUE);
            }
            SortedSet<MessageInfo> sortedSet;
            if (testOnly) {
                sortedSet = dut.getTestMessages();
            } else {
                sortedSet = dut.getSubjectsOfNewMessages(mailAccount);
            }
            System.out.println("new messages: " + sortedSet.size());
            sortedSet = dut.filterOnLastCheck(now, sortedSet);
            System.out.println("messages after: " + sortedSet.size());
            Multimap<String, String> map = dut.binMessageInfos(sortedSet);
            String message = dut.toMessage(map);
            LOG.info("send\n:" + message);
            if (!testOnly) {
                dut.sendWithSignal(message);
                dut.setLastCheck(now);
            }
        }
        // OkHttp3 hangs on Http2: This will be fixed (only) in OkHttp3 version 4.5.1-RC1, see
        // https://github.com/square/okhttp/issues/5832
        // https://github.com/square/okhttp/issues/4029
        System.exit(0);
    }

    private final boolean testOnly;
    private Preferences prefs;
    private Session session;
    private SimpleCollection collection;
    private Signal manager;

    public MailFetch(boolean testOnly) {
        this.testOnly = testOnly;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        prefs = Preferences.userRoot().node(KEYRING_COLLECTION_NAME);
    }

    public SortedSet<MessageInfo> getTestMessages() {
        SortedSet<MessageInfo> result = new TreeSet<>();
        result.add(new MessageInfo(Instant.parse("2014-12-12T10:39:40Z"), "hello 1"));
        result.add(new MessageInfo(Instant.parse("2016-12-12T10:39:40Z"), "hello 2"));
        result.add(new MessageInfo(Instant.parse("2018-01-12T10:39:40Z"), "hello 3"));
        result.add(new MessageInfo(Instant.parse("2018-01-12T10:39:39Z"), "hello 4"));
        return result;
    }

    private String toMessage(Multimap<String, String> map) {
        StringBuilder result = new StringBuilder();
        int entries = 0;
        LOOP:
        for (String ago : map.keySet()) {
            Collection<String> subjects = map.get(ago);
            result.append(ago).append(":\n");
            for (String s : subjects) {
                result.append("\t").append(s).append("\n");
                ++entries;
                if (entries > MAX_ENTRIES) break LOOP;
            }
        }
        return result.toString();
    }

    public SortedSet<MessageInfo> getSubjectsOfNewMessages(String mailAccount)
            throws MessagingException, IOException {
        SortedSet<MessageInfo> result = new TreeSet<>();
        if (session == null) {
            session = Session.getDefaultInstance(new Properties());
        }
        Store store = session.getStore("imaps");
        store.connect("imap.googlemail.com", 993, mailAccount,
                new String(getPasswdFor(mailAccount)));
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Fetch unseen messages from inbox folder
        Message[] messages = inbox.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        for (Message message : messages) {
            result.add(new MessageInfo(message.getSentDate().getTime(), message.getSubject()));
        }
        return result;
    }

    private SortedSet<MessageInfo> filterOnLastCheck(long now, SortedSet<MessageInfo> sortedList) {
        long lastCheck = prefs.getLong(PREFERENCES_LAST_LOOKUP, Long.MIN_VALUE);
        return sortedList.headSet(new MessageInfo(lastCheck, ""));
    }

    private void setLastCheck(long now) {
        prefs.putLong(PREFERENCES_LAST_LOOKUP, now);
    }

    private Multimap<String, String> binMessageInfos(SortedSet<MessageInfo> messages) {
        Multimap<String, String> result = MultimapBuilder.treeKeys().treeSetValues().build();
        for (MessageInfo m : messages) {
            result.put(TimeAgo.using(m.getTimeInMillis()), m.getSubject());
        }
        return result;
    }

    private char[] getPasswdFor(String account) throws IOException {
        if (collection == null) {
            collection = new SimpleCollection(KEYRING_COLLECTION_NAME, KEYRING_COLLECTION_SECRET);
        }
        // return collection.getAttributes("/org/freedesktop/secrets/collection/imap2signal/1").toString();
        // { xdg:schema => org.gnome.keyring.Note }
        // return collection.getLabel("/org/freedesktop/secrets/collection/imap2signal/1");
        // email address
        // return collection.getSecrets().entrySet().iterator().next().toString();
        for (String objectPath : collection.getItems(Collections.emptyMap())) {
            if (account.equals(collection.getLabel(objectPath))) {
                return collection.getSecret(objectPath);
            }
        }
        throw new IllegalArgumentException("account " + account +
                " not found in keyring collection " + KEYRING_COLLECTION_NAME);
    }

    public void sendWithSignal(String message) throws IOException, EncapsulatedExceptions {
        String sender = new String(getPasswdFor(SIGNAL_ACCOUNT));
        String recipient = new String(getPasswdFor(SIGNAL_RECIPIENTS));
        if (manager == null) {
            manager = new Manager(sender,
                    System.getProperty("user.home") + "/" + SIGNAL_CONFIG_DIR);
            ((Manager) manager).init();
        }
        manager.sendMessage(message, Collections.emptyList(), recipient);
    }

    @Override
    public void close() {
        if (collection != null) {
            collection.close();
        }
        session = null;
        manager = null;
    }
}
