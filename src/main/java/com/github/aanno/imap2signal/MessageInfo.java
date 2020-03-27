package com.github.aanno.imap2signal;

import javax.annotation.Nonnull;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class MessageInfo implements Comparable<MessageInfo> {

    private final long timeInMillis;

    private final String subject;

    private final Address from;

    private final Address replyTo;

    private final List<Address> tos = new ArrayList<>();

    private final List<Address> ccs = new ArrayList<>();

    public MessageInfo(long timeInMillis, String subject, Address from, Address replyTo,
                       List<Address> tos, List<Address> ccs) {
        this.timeInMillis = timeInMillis;
        this.subject = subject;
        this.from = from;
        this.replyTo = replyTo;
        this.tos.addAll(tos);
        this.ccs.addAll(ccs);
    }

    public MessageInfo(Instant instant, String subject, Address from, Address replyTo,
                       List<Address> tos, List<Address> ccs) {
        this(instant.toEpochMilli(), subject, from, replyTo, tos, ccs);
    }

    public MessageInfo(Message message) throws MessagingException {
        this(message.getSentDate().getTime(), message.getSubject(), message.getFrom()[0], message.getReplyTo()[0],
                Arrays.asList(message.getRecipients(Message.RecipientType.TO)),
                Arrays.asList(message.getRecipients(Message.RecipientType.CC)));
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageInfo)) return false;
        MessageInfo that = (MessageInfo) o;
        return timeInMillis == that.timeInMillis &&
                subject.equals(that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeInMillis, subject);
    }

    @Override
    public int compareTo(@Nonnull MessageInfo o) {
        // reverse on purpose: Newest messages first!
        int result = Long.compare(o.timeInMillis, timeInMillis);
        if (result == 0) {
            result = subject.compareTo(o.subject);
        }
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MessageInfo.class.getSimpleName() + "[", "]")
                .add("timeInMillis=" + timeInMillis)
                .add("subject='" + subject + "'")
                .toString();
    }

}
