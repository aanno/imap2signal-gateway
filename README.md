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
