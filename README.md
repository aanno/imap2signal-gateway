# imap2signal-gateway

Work-in-progress tool to send notifications about unread messages from a (mail) IMAP 
account to a [signal](https://signal.org/) account.

## Features

* uses [keyring](https://de.wikipedia.org/wiki/Gnome_Keyring) for secrets/passwords
* integrates with dbus (planed)
* lightweight

## Quickstart

* register and validate an signal account with [signal-cli](https://github.com/AsamK/signal-cli)
* create a new (gnome) keyring collection called 'imap2signal' with the following notes/key-values/passwords
  + email address -> email IMAP password (the mail account you want notifications for)
  + 'sender' -> signal sender phone number (the signal account (phone number) you send notifications from)
  + 'recipient' -> signal recipient phone number (the signal account (phone number) you send notifications to)
  The easiest way to do this is by using the 'seahorse' UI for gnome keyring
* Build app/distribution with `gradle clean build`
* Install app/distribution with `./scripts/unzip-distribution.sh `
* Run imap2signal with `./scripts/run-with-modulepath.sh <email address>`
  
## Java 11 start 

```bash
gradle build --info -x test
cd imap2signal-gateway-0.0.1-SNAPSHOT
mv lib/dbus-java-2.7.jar ../../lib2/
rm lib/dbus-java-3.0.2.jar 
java -p ".:lib" -jar imap2signal-gateway-0.0.1-SNAPSHOT.jar
```

```bash
# currently not working
java -cp "../lib2/*" -p ".:lib" -m imap2signal/com.github.aanno.imap2signal.MailFetch
```

## TODOs

* Currently, OAuth2 is not supported (e.g. for Gmail accounts)
* Currently, the gnome keyring scheme allows getting notification from several mail accounts _but_ only from _one_ signal 'sender' to _one_ signal recipient

## Links

* [secret-service](https://github.com/swiesend/secret-service) library for gnome-keyring (seahorse) access over D-Bus
* [signal-cli](https://github.com/AsamK/signal-cli) cli (and library) for signal (messenger app)
* [kdewallet](https://github.com/purejava/kdewallet) library for KDE Wallet access (planned)

