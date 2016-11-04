//
// Nashorn / Rhino compatible
//
if (typeof Java == 'undefined') {
  Java = {
      type : function(className) {
        var path = className.split(/\./g);
        var cls = Packages;
        for (var i = 0; i < path.length; i += 1) {
          cls = cls[path[i]];
        }
        return cls;
      }
  };
}

(function() {

  var System = Java.type('java.lang.System');
  var SpyDriver = Java.type('spylog.SpyDriver');
  var Connection = Java.type('java.sql.Connection');
  var Statement = Java.type('java.sql.Statement');

  var console = {
    log : function(msg) {
      System.out.println('[spylog]' + msg);
    }
  };

  var handler = {
    enter : function(targetUrl, target, methodName, args) {
      var sql = null;
      if (target instanceof Connection) {
        if ( (methodName == 'prepareStatement' ||
            methodName == 'prepareCall') &&
              args && args.length > 0) {
          sql = '' + args[0];
        }
      } else if (target instanceof Statement) {
        if ( (methodName == 'execute' ||
            methodName == 'executeQuery' || 
            methodName == 'executeUpdate') &&
              args && args.length > 0) {
          sql = '' + args[0];
        }
      }
      if (sql) {
        console.log(targetUrl + ' - ' + methodName + ' - ' +
            sql.replace(/\s+/g, ' ') );
      }
    },
    result : function(targetUrl, target, methodName, result) {},
    throwable : function(targetUrl, target, methodName, t) {},
    exit : function(targetUrl, target, methodName) {}
  };

  return new SpyDriver.Handler(handler);
}());
