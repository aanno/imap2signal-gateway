# Links for developement

## java mail

* [mstor](https://github.com/benfortuna/mstor) mbox based mail storage for javax.mail

### javax.mail

* [faq](https://javaee.github.io/javamail/FAQ)
* [push messages](https://stackoverflow.com/questions/4389994/does-javamail-support-server-push)
* [clear message cache](https://stackoverflow.com/questions/1466720/how-to-force-javamail-to-clear-its-message-cache)

## dns

* http://www.ioncannon.net/system-administration/58/using-java-to-get-detailed-dns-information/
* https://www.tomred.net/java/extended-email-validation-using-dns-mx-lookup.html
* https://www.rgagnon.com/javadetails/java-0452.html

## password stores

* [java library for windows credentials](https://github.com/dariusz-szczepaniak/java.jna.WindowsCredentialManager)
* [microsoft git credential manager](https://github.com/microsoft/Git-Credential-Manager-Core)
* [tmobile's t-vault](https://github.com/tmobile/t-vault) based on vault
* [keepassxc](https://keepassxc.org/project/) has [dbus support](https://github.com/keepassxreboot/keepassxc/wiki/Using-DBus-with-KeePassXC)
* [jPasswords](https://sourceforge.net/projects/jpws/)
* [master password](https://gitlab.com/MasterPassword/MasterPassword)
* [java multi platform library](https://github.com/joval/jKeyring)

### keyring

* [keyring overview](https://rtfm.co.ua/en/what-is-linux-keyring-gnome-keyring-secret-service-and-d-bus/)
* https://www.linux-magazin.de/ausgaben/2010/12/api-fuer-vertrauliches/
* [secret service](https://github.com/swiesend/secret-service) java library for using keyring
  + does _not_ work with kwallet: https://github.com/swiesend/secret-service/issues/8
* dbus-java
  + [dbus-java documentation](https://dbus.freedesktop.org/doc/dbus-java/)
  + [dbus-java](https://github.com/bdeneuter/dbus-java)
  + [dbus-java deprecated implementation](https://github.com/freedesktop/dbus-java)
  + [dbus-java new implementation](https://github.com/hypfvieh/dbus-java) but incompatible (!)
* security alerts:
  + https://nvd.nist.gov/vuln/detail/CVE-2018-19358
  + https://tools.cisco.com/security/center/viewAlert.x?alertId=59179

#### gnome-keyring

* [gnome keyring (archlinux)](https://wiki.archlinux.org/index.php/GNOME/Keyring)

#### kwallet

* [password extraction with perl](https://www.perlmonks.org/?node_id=869620)

## [vault](https://www.vaultproject.io/)

* [spring cloud and vault 1](https://blog.marcosbarbero.com/integrating-vault-spring-cloud-config/)
* [spring cloud and vault 2](http://work.haufegroup.io/spring-cloud-config-and-vault/)
* [spring vault](https://github.com/spring-projects/spring-vault)
* [java zero dependency vault library](https://github.com/BetterCloud/vault-java-driver)
* [java jvaultconnector](https://github.com/stklcode/jvaultconnector)

## signal

* [official signal java library](https://github.com/signalapp/libsignal-protocol-java)

### signal-cli

* [signal-cli](https://github.com/AsamK/signal-cli)
* [cli overview](https://github.com/AsamK/signal-cli/blob/master/man/signal-cli.1.adoc)
* [link to other devices](https://github.com/AsamK/signal-cli/wiki/Linking-other-devices-(Provisioning))
  + [link bug](https://github.com/AsamK/signal-cli/issues/277)
* [dbus support](https://github.com/AsamK/signal-cli/wiki/DBus-service)
