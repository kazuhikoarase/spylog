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
  var getSqlCommand = function(target, methodName, args) {
    if (target instanceof Connection) {
      if ( (methodName == 'prepareStatement' ||
          methodName == 'prepareCall') &&
            args && args.length > 0) {
        return '' + args[0];
      }
    } else if (target instanceof Statement) {
      if ( (methodName == 'execute' ||
          methodName == 'executeQuery' || 
          methodName == 'executeUpdate') &&
            args && args.length > 0) {
        return '' + args[0];
      }
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
        var opts = {
          msg : targetUrl + ' - ' + methodName + ' - ' +
          sql.replace(/\s+/g, ' '),
          startTime : System.currentTimeMillis()
        };
        console.log(' => ' + opts.msg);
        return opts;
      }
      return null;
    },
    result : function(opts, result) {},
    throwable : function(opts, t) {},
    exit : function(opts) {
      if (opts != null) {
        console.log(' <= ' + opts.msg + ' ' +
            (System.currentTimeMillis() - opts.startTime) + 'ms');
      }
    }
  };

  return new SpyDriver.Handler(handler);
}());
