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
    result : function() {},
    throwable : function() {},
    exit : function() {}
  };

  return new Packages.spylog.SpyDriver.Handler(handler);
}());
