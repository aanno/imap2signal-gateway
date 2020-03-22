package com.github.aanno.imap2signal;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalField;
import java.util.Objects;
import java.util.StringJoiner;

public final class MessageInfo implements Comparable<MessageInfo> {

    private final long timeInMillis;

    private final String subject;

    public MessageInfo(long timeInMillis, String subject) {
        this.timeInMillis = timeInMillis;
        this.subject = subject;
    }

    public MessageInfo(Instant instant, String subject) {
        this(instant.toEpochMilli(), subject);
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
    public int compareTo(@NotNull MessageInfo o) {
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
