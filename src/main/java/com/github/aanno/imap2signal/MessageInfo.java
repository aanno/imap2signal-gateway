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
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class MessageInfo implements Comparable<MessageInfo> {

    private final long timeInMillis;

    private final String subject;

    private final SimpleMailAddress from;

    private final SimpleMailAddress replyTo;

    private final Set<SimpleMailAddress> tos = new TreeSet<>();

    private final Set<SimpleMailAddress> ccs = new TreeSet<>();

    private static Address addressOrNull(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses.length > 1) {
            throw new IllegalArgumentException("" + Arrays.asList(addresses));
        }
        return addresses[0];
    }

    public MessageInfo(long timeInMillis, String subject, Address from, Address replyTo,
                       List<Address> tos, List<Address> ccs) {
        this.timeInMillis = timeInMillis;
        this.subject = subject;
        this.from = SimpleMailAddress.from(from);
        this.replyTo = SimpleMailAddress.from(replyTo);
        this.tos.addAll(tos.stream().map(SimpleMailAddress::from).collect(Collectors.toList()));
        this.ccs.addAll(ccs.stream().map(SimpleMailAddress::from).collect(Collectors.toList()));
    }

    public MessageInfo(Instant instant, String subject, Address from, Address replyTo,
                       List<Address> tos, List<Address> ccs) {
        this(instant.toEpochMilli(), subject, from, replyTo, tos, ccs);
    }

    public MessageInfo(Message message) throws MessagingException {
        this(message.getSentDate().getTime(), message.getSubject(),
                addressOrNull(message.getFrom()),
                addressOrNull(message.getReplyTo()),
                Arrays.asList(message.getRecipients(Message.RecipientType.TO)),
                Arrays.asList(message.getRecipients(Message.RecipientType.CC)));
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getSubject() {
        return subject;
    }

    public SimpleMailAddress getFrom() {
        return from;
    }

    public SimpleMailAddress getReplyTo() {
        return replyTo;
    }

    public Set<SimpleMailAddress> getTos() {
        return tos;
    }

    public Set<SimpleMailAddress> getCcs() {
        return ccs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageInfo)) return false;
        MessageInfo that = (MessageInfo) o;
        return timeInMillis == that.timeInMillis &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(from, that.from) &&
                Objects.equals(replyTo, that.replyTo) &&
                tos.equals(that.tos) &&
                ccs.equals(that.ccs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeInMillis, subject, from, replyTo, tos, ccs);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MessageInfo.class.getSimpleName() + "[", "]")
                .add("subject='" + subject + "'")
                .add("from=" + from)
                .add("replyTo=" + replyTo)
                .add("tos=" + tos)
                .add("ccs=" + ccs)
                .toString();
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
}
