# imap2signal-gateway

Work-in-progress tool to send notifications about unread messages from a (mail) IMAP 
account to a [signal](https://signal.org/) account.

## Features

* uses [keyring](https://de.wikipedia.org/wiki/Gnome_Keyring) for secrets/passwords
* integrates with dbus (planed)
* lightweight

## Quickstart

* register and validate an signal account with [signal-cli](https://github.com/AsamK/signal-cli)
* create a new keyring collection called 'imap2signal' with the following notes/key-values/passwords
  + email address -> email IMAP password
  + 'sender' -> signal sender phone number
  + 'recipient' -> signal recipient phone number
  
## Java 11 start 

```bash
mv lib/dbus-java-2.7.jar ../../lib2/
java -cp "../../lib2/*" -p ".:lib" -m imap2signal/com.github.aanno.imap2signal.MailFetch
```

```bash
java -p ".:lib" -jar imap2signal-gateway-0.0.1-SNAPSHOT.jar
```

## Links

* [secret-service](https://github.com/swiesend/secret-service) library for gnome-keyring (seahorse) access over D-Bus
* [signal-cli](https://github.com/AsamK/signal-cli) cli (and library) for signal (messenger app)
* [kdewallet](https://github.com/purejava/kdewallet) library for KDE Wallet access (planned)

