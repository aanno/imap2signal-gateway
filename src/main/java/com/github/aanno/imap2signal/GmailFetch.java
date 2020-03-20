package com.github.aanno.imap2signal;

import org.asamk.Signal;
import org.asamk.signal.manager.Manager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * https://stackoverflow.com/questions/28689099/javamail-reading-recent-unread-mails-using-imap
 */
public class GmailFetch implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GmailFetch.class);

    private static final String KEYRING_COLLECTION_NAME = "imap2signal";
    private static final String KEYRING_COLLECTION_SECRET = "secret";

    private static final String SIGNAL_CONFIG_DIR = ".local/share/signal-cli";

    private static final String MAIL_ACCOUNT = "aannoaanno@gmail.com";
    private static final String SIGNAL_ACCOUNT = "sender";
    private static final String SIGNAL_RECIPIENTS = "recipient";

    public static void main(String[] args) throws Exception, EncapsulatedExceptions {
        try (GmailFetch dut = new GmailFetch()) {
            List<String> list = dut.getSubjectsOfNewMessages();
            // List<String> list = dut.getTestMessages();
            if (list.size() >= 40) {
                list = list.subList(0, 40);
            }
            for (String s : list) {
                System.out.println(s);
            }
            dut.sendWithSignal(list.stream().collect(Collectors.joining("\n")));
        }
    }

    private Session session;
    private SimpleCollection collection;
    private Signal manager;

    public GmailFetch() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public List<String> getTestMessages() {
        List<String> result = new ArrayList<>();
        result.add("hello");
        result.add("world");
        return result;
    }

    public List<String> getSubjectsOfNewMessages() throws MessagingException, IOException {
        List result = new ArrayList();
        if (session == null) {
            session = Session.getDefaultInstance(new Properties());
        }
        Store store = session.getStore("imaps");
        store.connect("imap.googlemail.com", 993, MAIL_ACCOUNT, new String(getPasswdFor(MAIL_ACCOUNT)));
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Fetch unseen messages from inbox folder
        Message[] messages = inbox.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        // Sort messages from recent to oldest
        Arrays.sort(messages, (m1, m2) -> {
            try {
                return m2.getSentDate().compareTo(m1.getSentDate());
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

        for (Message message : messages) {
            result.add(
                    "sendDate: " + message.getSentDate()
                            + " subject:" + message.getSubject());
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
    }
}
