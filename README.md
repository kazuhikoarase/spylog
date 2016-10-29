spylog
===

Place spylog.jar and spylog.js with your driver.

```
foo-bar-jdbc-driver.jar
spylog.jar
spylog.js
```

Then modify your jdbc property as follows.

* original
```
driverClassName=<foo>
url=<jdbc:bar>
```

* modified
```
driverClassName=spylog.SpyDriver
url=jdbc:spylog:<foo>/<jdbc:bar>
```

Now, you can get all the sql statements in logfile! :).
