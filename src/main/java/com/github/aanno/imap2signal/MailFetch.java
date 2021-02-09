package com.github.aanno.imap2signal;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.asamk.Signal;
import org.asamk.signal.BaseConfig;
import org.asamk.signal.manager.Manager;
import org.asamk.signal.manager.config.ServiceEnvironment;
import org.asamk.signal.manager.NotRegisteredException;
import org.asamk.signal.manager.AttachmentInvalidException;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.RejectedExecutionException;
import java.util.prefs.Preferences;

/**
 * https://stackoverflow.com/questions/28689099/javamail-reading-recent-unread-mails-using-imap
 */
public class MailFetch implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MailFetch.class);

    private final static String USER_AGENT = BaseConfig.PROJECT_NAME == null ?
            "signal-cli" : BaseConfig.PROJECT_NAME + " " + BaseConfig.PROJECT_VERSION;

    private static final int MAX_ENTRIES = 40;
    private static String[] SUBDOMAINS_TO_TRY = new String[]{"imap.", "mail."};

    private static final String KEYRING_COLLECTION_NAME = "imap2signal";
    private static final String KEYRING_COLLECTION_SECRET = "secret";
    private static final String PREFERENCES_LAST_LOOKUP = "timeMillisOfLastCheck";

    private static final String SIGNAL_CONFIG_DIR = ".local/share/signal-cli";

    private static final String MAIL_ACCOUNT = "aannoaanno@gmail.com";
    private static final String SIGNAL_ACCOUNT = "sender";
    private static final String SIGNAL_RECIPIENTS = "recipient";

    public static void main(String[] args) throws Throwable {
        String mailAccount = MAIL_ACCOUNT;
        boolean testOnly = false;
        if (args.length > 0) {
            mailAccount = args[0];
        }
        if ("test".equals(mailAccount)) {
            testOnly = true;
        }
        SimpleMailAddress myMailAddr = new SimpleMailAddress(mailAccount);
        LOG.info("mail account: " + myMailAddr + " testOnly: " + testOnly);
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
                sortedSet = dut.getSubjectsOfNewMessages(myMailAddr);
            }
            System.out.println("new messages: " + sortedSet.size());
            sortedSet = dut.filterOnLastCheck(now, sortedSet);
            System.out.println("messages after: " + sortedSet.size());
            if (!sortedSet.isEmpty()) {
                Multimap<HumanRelativeDate, MessageInfo> map = dut.binMessageInfos(sortedSet);
                String message = dut.toMessage(map, myMailAddr);
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
        try {
            System.exit(0);
        } catch (RejectedExecutionException e) {
            // do nothing, because of
            /*
            SimpleCollection -> TransportEncryption -> DBusConnection missing .close
            as SimpleCollection.close() only clear(), but not close the connection

            Exception in thread "DBusConnection" java.util.concurrent.RejectedExecutionException: Task org.freedesktop.dbus.connections.AbstractConnection$1@7682e526 rejected from java.util.concurrent.ThreadPoolExecutor@710fe758[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 32]
	at java.base/java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2055)
	at java.base/java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:825)
	at java.base/java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1355)
	at org.freedesktop.dbus.connections.AbstractConnection.sendMessage(AbstractConnection.java:317)
	at org.freedesktop.dbus.connections.AbstractConnection.handleMessage(AbstractConnection.java:959)
	at org.freedesktop.dbus.connections.AbstractConnection.handleMessage(AbstractConnection.java:619)
	at org.freedesktop.dbus.connections.IncomingMessageThread.run(IncomingMessageThread.java:43)
            */
        }
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
    private Manager manager;

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

    private String toMessage(Multimap<HumanRelativeDate, MessageInfo> map, SimpleMailAddress myMailAddr) {
        StringBuilder result = new StringBuilder();
        result.append(
                SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new Date()))
                .append(":\n");
        int entries = 0;
        LOOP:
        for (HumanRelativeDate ago : map.keySet()) {
            Collection<MessageInfo> info = map.get(ago);
            result.append(ago.getHumanDate()).append(":\n");
            for (MessageInfo mi : info) {
                // sender
                result.append(mi.getFrom());
                if (mi.getReplyTo() != null && !mi.getReplyTo().equals(mi.getFrom())) {
                    String rt = mi.getReplyTo().getLocalPart();
                    if (rt.length() > 15) {
                        rt = rt.substring(0, 12) + "...";
                    }
                    result.append("(").append(rt).append(")");
                }
                result.append(": ");
                // subject
                String s = mi.getSubject();
                if (s.length() > 50) {
                    s = s.substring(0, 47) + "...";
                }
                result.append("\t").append(s).append(" ");
                // recipients
                if (!mi.getTos().isEmpty()) {
                    result.append("t").append(mi.getTos().size());
                    if (mi.getTos().contains(myMailAddr)) {
                        result.append("*");
                    }
                }
                if (!mi.getCcs().isEmpty()) {
                    result.append("c").append(mi.getCcs().size());
                    if (mi.getCcs().contains(myMailAddr)) {
                        result.append("*");
                    }
                }
                // end line
                result.append("\n");
                ++entries;
                if (entries > MAX_ENTRIES) break LOOP;
            }
        }
        return result.toString();
    }

    public SortedSet<MessageInfo> getSubjectsOfNewMessages(SimpleMailAddress mailAccount)
            throws MessagingException, IOException {
        SortedSet<MessageInfo> result = new TreeSet<>();
        if (session == null) {
            // TODO aanno: OAuth2 support (for gmail), see https://eclipse-ee4j.github.io/mail/OAuth2
            session = Session.getDefaultInstance(new Properties());
        }
        Store store = session.getStore("imaps");
        String domain = mailAccount.getDomain();
        String pw = new String(getPasswdFor(mailAccount.toString()));
        /*
        store.connect("imap.googlemail.com", 993, mailAccount,
                new String(getPasswdFor(mailAccount)));
                */
        MessagingException last = null;
        for (String sub : SUBDOMAINS_TO_TRY) {
            String mailHost = sub + domain;
            LOG.info("imap connect: trying " + mailHost + " ...");
            try {
                store.connect(mailHost, 993, mailAccount.toString(), pw);
                LOG.info("imap connected to " + mailHost);
                last = null;
                // using that ...
                break;
            } catch (MessagingException e) {
                last = e;
                LOG.info(mailHost + " failed: " + e);
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

    private Multimap<HumanRelativeDate, MessageInfo> binMessageInfos(SortedSet<MessageInfo> messages) {
        Multimap<HumanRelativeDate, MessageInfo> result = MultimapBuilder.treeKeys().treeSetValues().build();
        for (MessageInfo m : messages) {
            result.put(new HumanRelativeDate(m.getTimeInMillis()), m);
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

    public void sendWithSignal(String message) throws IOException,
            NotRegisteredException, AttachmentInvalidException, InvalidNumberException {
        String sender = new String(getPasswdFor(SIGNAL_ACCOUNT));
        String recipient = new String(getPasswdFor(SIGNAL_RECIPIENTS));
        if (manager == null) {
            // manager = new Manager(sender,
            //         System.getProperty("user.home") + "/" + SIGNAL_CONFIG_DIR);
            manager = Manager.init(sender,
                    new File(System.getProperty("user.home"), SIGNAL_CONFIG_DIR),
                    ServiceEnvironment.LIVE,
                    USER_AGENT);
        }
        manager.sendMessage(message, Collections.emptyList(), Collections.singletonList(recipient));
    }

    @Override
    public void close() {
        if (collection != null) {
            // TODO tp: Bug in close
            collection.close();
        }
        session = null;
        manager = null;
    }
}
