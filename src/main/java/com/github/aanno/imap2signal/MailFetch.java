package com.github.aanno.imap2signal;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.asamk.Signal;
import org.asamk.signal.manager.Manager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.security.Security;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

/**
 * https://stackoverflow.com/questions/28689099/javamail-reading-recent-unread-mails-using-imap
 */
public class MailFetch implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MailFetch.class);

    private static final int MAX_ENTRIES = 40;
    private static String[] SUBDOMAINS_TO_TRY = new String[]{"imap.", "mail."};

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
            if (!sortedSet.isEmpty()) {
                Multimap<HumanRelativeDate, String> map = dut.binMessageInfos(sortedSet);
                String message = dut.toMessage(map);
                LOG.info("send\n:" + message);
                if (!testOnly) {
                    dut.sendWithSignal(message);
                    dut.setLastCheck(now);
                }
            }
        }
        // We need a grace period before exit for DBus connection to stop
        Thread.sleep(2000);
        // OkHttp3 hangs on Http2: This will be fixed (only) in OkHttp3 version 4.5.1-RC1, see
        // https://github.com/square/okhttp/issues/5832
        // https://github.com/square/okhttp/issues/4029
        System.exit(0);
    }

    private static class MyAddress extends Address {

        private String addr;

        MyAddress(String addr) {
            this.addr = addr;
        }

        @Override
        public String getType() {
            return "MyAddress";
        }

        @Override
        public String toString() {
            return addr;
        }

        @Override
        public int hashCode() {
            return addr.hashCode() * 3 + 31;
        }

        @Override
        public boolean equals(Object address) {
            if (!(address instanceof  Address)) {
                return false;
            }
            Address other = (Address) address;
            return toString().equals(other.toString());
        }
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
        result.add(getTestMessageInfo("2014-12-12T10:39:40Z", "hello 1",
                "my@tiger.cx", "tp@myplace.de"));
        result.add(getTestMessageInfo("2016-12-12T10:39:40Z", "hello 2",
                "myme@tiger.cx", "tp@myplace.de"));
        result.add(getTestMessageInfo("2018-01-12T10:39:40Z", "hello 3",
                "my@tiger.cx", "tp@myplace.de"));
        result.add(getTestMessageInfo("2018-01-12T10:39:39Z", "hello 4",
                "my@tiger.cx", "tp@myplace.de"));
        return result;
    }

    private MessageInfo getTestMessageInfo(String date, String subject, String from, String tos) {
        return getTestMessageInfo(Instant.parse(date).toEpochMilli(), subject, from, tos);
    }

    private MessageInfo getTestMessageInfo(long epochMilli, String subject, String from, String tos) {
        MyAddress fromAddr = new MyAddress(from);
        MyAddress toAddr = new MyAddress(tos);
        return new MessageInfo(epochMilli, subject, fromAddr, fromAddr,
                Collections.singletonList(toAddr), Collections.emptyList());
    }

    private String toMessage(Multimap<HumanRelativeDate, String> map) {
        StringBuilder result = new StringBuilder();
        int entries = 0;
        LOOP:
        for (HumanRelativeDate ago : map.keySet()) {
            Collection<String> subjects = map.get(ago);
            result.append(ago.getHumanDate()).append(":\n");
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
        String domain = mailAccount.substring(mailAccount.lastIndexOf("@") + 1);
        String pw = new String(getPasswdFor(mailAccount));
        /*
        store.connect("imap.googlemail.com", 993, mailAccount,
                new String(getPasswdFor(mailAccount)));
                */
        MessagingException last = null;
        for (String sub : SUBDOMAINS_TO_TRY) {
            String mailHost = sub + domain;
            LOG.info("imap connect: trying " + mailHost + " ...");
            try {
                store.connect(mailHost, 993, mailAccount, pw);
                LOG.info("imap connected to " + mailHost);
                last = null;
                // using that ...
                break;
            } catch (MessagingException e) {
                last = e;
                // try again
            }
        }
        if (last != null) {
            LOG.info("imap connection failed");
            throw last;
        }

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Fetch unseen messages from inbox folder
        Message[] messages = inbox.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        for (Message message : messages) {
            /*
            message.getAllRecipients();
            message.getRecipients(Message.RecipientType.CC);
            message.getSentDate();
            message.getReplyTo();
            message.getFrom();
             */
            result.add(new MessageInfo(message));
        }
        return result;
    }

    private SortedSet<MessageInfo> filterOnLastCheck(long now, SortedSet<MessageInfo> sortedList) {
        long lastCheck = prefs.getLong(PREFERENCES_LAST_LOOKUP, Long.MIN_VALUE);
        return sortedList.headSet(getTestMessageInfo(lastCheck, "",
                "local@domain.com", "local@domain.com"));
    }

    private void setLastCheck(long now) {
        prefs.putLong(PREFERENCES_LAST_LOOKUP, now);
    }

    private Multimap<HumanRelativeDate, String> binMessageInfos(SortedSet<MessageInfo> messages) {
        Multimap<HumanRelativeDate, String> result = MultimapBuilder.treeKeys().treeSetValues().build();
        for (MessageInfo m : messages) {
            result.put(new HumanRelativeDate(m.getTimeInMillis()), m.getSubject());
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
