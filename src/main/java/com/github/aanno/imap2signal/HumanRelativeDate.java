package com.github.aanno.imap2signal;

import com.github.marlonlom.utilities.timeago.TimeAgo;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.StringJoiner;

public final class HumanRelativeDate implements Comparable<HumanRelativeDate> {

    private final long timeInMillis;
    private final String humanDate;

    public HumanRelativeDate(long timeInMillis, String humanDate) {
        this.timeInMillis = timeInMillis;
        this.humanDate = humanDate;
    }

    public HumanRelativeDate(long timeInMillis) {
        this(timeInMillis, TimeAgo.using(timeInMillis));
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getHumanDate() {
        return humanDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HumanRelativeDate)) return false;
        HumanRelativeDate that = (HumanRelativeDate) o;
        return timeInMillis == that.timeInMillis &&
                humanDate.equals(that.humanDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeInMillis, humanDate);
    }

    @Override
    public int compareTo(@Nonnull HumanRelativeDate o) {
        // reverse on purpose: Newest messages first!
        int result = Long.compare(o.timeInMillis, timeInMillis);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HumanRelativeDate.class.getSimpleName() + "[", "]")
                .add("timeMillis=" + timeInMillis)
                .add("humanDate='" + humanDate + "'")
                .toString();
    }

}
