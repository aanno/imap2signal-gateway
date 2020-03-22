# Basic useage of signal-cli

* https://github.com/AsamK/signal-cli/blob/master/man/signal-cli.1.adoc

## Registering new device

* https://github.com/AsamK/signal-cli/wiki/Linking-other-devices-(Provisioning)
* https://mark.benschop.me/blog/?p=122

```bash
$ gradle run -PappArgs="['link', '-n', '<name of device>']"
> Task :run
tsdevice:/?uuid=BNj_YoA2E3MsxTmTtQvJSw&pub_key=BWpoUXmlzL4qhYCNmnI4U8OQ9Qmlj3aLN1kAc11NOjYi

$ qrencode -t png -o qr.png 'tsdevice:/?uuid=BNj_YoA2E3MsxTmTtQvJSw&pub_key=BWpoUXmlzL4qhYCNmnI4U8OQ9Qmlj3aLN1kAc11NOjYi'
```

Display the qr-code with an image viewer and scan it with the (mobile) signal app. If the 
'master decive' is `signal-cli` you could use the uri directly like this:

```bash
$ gradle run -PappArgs="['-u', '+49*******', 'addDevice', '--uri', 'tsdevice:/?uuid=UPTuGon*******&pub_key=BRL%2BslGfVmtF%2F3pBwdlVJz*************']"
```

## Setup an old POTS device

```bash
$ gradle run -PappArgs="['-u', '+49*******', 'register', '-v']"
```

Wait for voice incoming call with verification number.

```bash
$ gradle run -PappArgs="['-u', '+49*******', 'verify', '332985']"
```
Add '-p PIN' if PIN is set.

```bash
$ gradle run -PappArgs="['-u', '+49*******', 'setPin', '****']"
Exception in thread "main" java.lang.RuntimeException: Not implemented anymore, will be replaced with KBS
        at org.asamk.signal.manager.Manager.setRegistrationLockPin(Manager.java:412)
        at org.asamk.signal.commands.SetPinCommand.handleCommand(SetPinCommand.java:27)
        at org.asamk.signal.Main.handleCommands(Main.java:130)
        at org.asamk.signal.Main.main(Main.java:59)
```

## Send a message

```bash
$ gradle run -PappArgs="['-u', '+49*******', 'send', '+49*******', '-m', 'Hello world from signal-cli']"
```
