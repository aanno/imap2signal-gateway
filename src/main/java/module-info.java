open module imap2signal {
    requires timeago;
    requires com.google.common;
    requires signal.cli;
    requires signal.lib;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.bouncycastle.provider;
    requires secret.service;
    requires org.slf4j;
    // requires org.slf4j.simple;
    // requires slf4j.jdk14;
    // requires slf4j.api;
    requires jakarta.mail;
    requires jakarta.activation;
    requires java.prefs;
    // needed for sun.misc.SignalHandler
    requires jdk.unsupported;
    requires hkdf;
    requires jsr305;
    requires signal.client.java;
    requires signal.service.java;
    requires emailaddress.rfc2822;
}
