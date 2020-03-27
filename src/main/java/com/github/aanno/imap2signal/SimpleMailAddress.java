package com.github.aanno.imap2signal;

import org.hazlewood.connor.bottema.emailaddress.EmailAddressCriteria;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.mail.Address;
import java.util.EnumSet;
import java.util.Objects;
import java.util.StringJoiner;

public final class SimpleMailAddress implements Comparable<SimpleMailAddress> {

    public static SimpleMailAddress from(Address address) {
        if (address == null) {
            return null;
        }
        return new SimpleMailAddress(address.toString());
    }

    private final String localPart;

    private final String domain;

    public SimpleMailAddress(@Nonnull String localPart, @Nonnull String domain) {
        this.localPart = localPart;
        this.domain = domain;
    }

    public SimpleMailAddress(String unparsed, @Nonnull EnumSet<EmailAddressCriteria> criteria, boolean extractCfwsPersonalNames) {
        this(EmailAddressParser.getLocalPart(unparsed, criteria, extractCfwsPersonalNames),
                EmailAddressParser.getDomain(unparsed, criteria, extractCfwsPersonalNames));
    }

    public SimpleMailAddress(@Nonnull String unparsed) {
        this(unparsed, EmailAddressCriteria.DEFAULT, true);
    }

    public String getLocalPart() {
        return localPart;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleMailAddress)) return false;
        SimpleMailAddress that = (SimpleMailAddress) o;
        return localPart.equals(that.localPart) &&
                domain.equals(that.domain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPart, domain);
    }

    @Override
    public int compareTo(@NotNull SimpleMailAddress o) {
        int result = domain.compareTo(o.domain);
        if (result == 0) {
            result = localPart.compareTo(o.localPart);
        }
        return result;
    }

    @Override
    public String toString() {
        return localPart + "@" + domain;
    }

}
