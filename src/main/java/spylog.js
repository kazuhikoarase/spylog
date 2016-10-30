(function() {

  var console = {
    log : function(msg) {
      java.lang.System.out.println('[spylog]' + msg);
    }
  };

  var handler = {
    enter : function(targetUrl, target, methodName, args) {
      var sql = null;
      if (target instanceof java.sql.Connection) {
        if ( (methodName == 'prepareStatement' ||
            methodName == 'prepareCall') &&
              args && args.length > 0) {
          sql = '' + args[0];
        }
      } else if (target instanceof java.sql.Statement) {
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

  return new Packages.spylog.SpyDriver.Handler(handler);
}());
