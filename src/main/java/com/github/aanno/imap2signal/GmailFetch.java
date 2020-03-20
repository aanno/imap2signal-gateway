package com.github.aanno.imap2signal;

import org.freedesktop.secret.simple.SimpleCollection;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.*;

/**
 * https://stackoverflow.com/questions/28689099/javamail-reading-recent-unread-mails-using-imap
 */
public class GmailFetch {

    private static final String KEYRING_COLLECTION_NAME = "imap2signal";
    private static final String KEYRING_COLLECTION_SECRET = "secret";

    private static final String ACCOUNT = "aannoaanno@gmail.com";

    public static void main(String[] args) throws Exception {
        GmailFetch dut = new GmailFetch();
        dut.getSubjectsOfNewMessages();
        // System.out.println(dut.getPasswdFor(ACCOUNT));
    }

    public GmailFetch() {
    }

    public void getSubjectsOfNewMessages() throws MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties());
        Store store = session.getStore("imaps");
        store.connect("imap.googlemail.com", 993, ACCOUNT, new String(getPasswdFor(ACCOUNT)));
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
            System.out.println(
                    "sendDate: " + message.getSentDate()
                            + " subject:" + message.getSubject());
        }
    }

    private char[] getPasswdFor(String account) throws IOException {
        SimpleCollection collection = new SimpleCollection(KEYRING_COLLECTION_NAME, KEYRING_COLLECTION_SECRET);
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

    private void sendWithSignal() {

    }
}
