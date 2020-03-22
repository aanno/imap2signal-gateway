package com.github.aanno.imap2signal;

import com.github.marlonlom.utilities.timeago.TimeAgo;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.StringJoiner;

public final class HumanRelativeDate implements Comparable<HumanRelativeDate> {

    private final long timeMillis;
    private final String humanDate;

    public HumanRelativeDate(long timeMillis, String humanDate) {
        this.timeMillis = timeMillis;
        this.humanDate = humanDate;
    }

    public HumanRelativeDate(long timeMillis) {
        this(timeMillis, TimeAgo.using(timeMillis));
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getHumanDate() {
        return humanDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HumanRelativeDate)) return false;
        HumanRelativeDate that = (HumanRelativeDate) o;
        return timeMillis == that.timeMillis &&
                humanDate.equals(that.humanDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeMillis, humanDate);
    }

    @Override
    public int compareTo(@Nonnull HumanRelativeDate o) {
        // reverse on purpose: Newest messages first!
        int result = Long.compare(o.timeMillis, timeMillis);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HumanRelativeDate.class.getSimpleName() + "[", "]")
                .add("timeMillis=" + timeMillis)
                .add("humanDate='" + humanDate + "'")
                .toString();
    }

}
