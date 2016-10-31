package spylog;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.script.ScriptEngineManager;

/**
 * SpyDriver
 * @author Kazuhiko Arase
 */
public class SpyDriver implements Driver {

  private static final String JDBC_PREFIX = "jdbc:spylog:";

  private static final String JS_PATH = "/spylog.js";

  public interface Handler {
    void enter(String targetUrl, Object target,
        String methodName, Object[] args);
    void result(String targetUrl, Object target,
        String methodName, Object result);
    void exit(String targetUrl, Object target, String methodName);
    void throwable(String targetUrl, Object target,
        String methodName, Throwable t);
  }

  private static final Handler defaultHandler = new Handler() {
    @Override
    public void enter(String targetUrl, Object target,
        String methodName, Object[] args) {
    }
    @Override
    public void result(String targetUrl, Object target,
        String methodName, Object result) {
    }
    @Override
    public void throwable(String targetUrl, Object target,
        String methodName, Throwable t) {
    }
    @Override
    public void exit(String targetUrl, Object target,
        String methodName) {
    }
  };

  private static Handler handler;

  static {
    try {
      DriverManager.registerDriver(new SpyDriver() );
    } catch(SQLException e) {
      // ignore
    }
    loadHandler();
  }

  /**
   * reloadable.
   */
  public static void loadHandler() {
    URL url = SpyDriver.class.getResource(JS_PATH);
    if (url == null) {
      url = SpyDriver.class.getResource(JS_PATH.substring(1) );
    }
    if (url == null) {
      handler = defaultHandler;
    } else {
      try {
        final Reader in = new InputStreamReader(
            url.openStream(), "UTF-8");
        try {
          handler = (Handler)new ScriptEngineManager().
              getEngineByName("javascript").eval(in);
        } finally {
          in.close();
        }
      } catch(Exception e) {
        e.printStackTrace();
        handler = defaultHandler;
      }
    }
  }

  public SpyDriver() {
  }

  @Override
  public boolean acceptsURL(final String url) throws SQLException {
    return get(url, false, new Getter<Boolean>() {
      public Boolean get(
        final Driver driver,
        final String targetUrl
      ) throws SQLException {
        return driver.acceptsURL(targetUrl);
      }
    });
  }

  @Override
  public Connection connect(
    final String url, 
    final Properties info
  ) throws SQLException {
    return get(url, null, new Getter<Connection>() {
      public Connection get(
        final Driver driver,
        final String targetUrl
      ) throws SQLException {
        final Connection conn = driver.connect(targetUrl, info);
        return Invoker.newProxy(targetUrl, conn, Connection.class);
      }
    } );
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(
    final String url,
    final Properties info
  ) throws SQLException {
    return get(url, 
      new DriverPropertyInfo[0], 
      new Getter<DriverPropertyInfo[]>() {
        public DriverPropertyInfo[] get(
          final Driver driver,
          final String targetUrl
        ) throws SQLException {
          return driver.getPropertyInfo(targetUrl, info);
        }
    } );
  }

  @Override
  public int getMajorVersion() {
    return 1;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  //@Override // jdk1.7 or later
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  private static <T> T get(
    final String url,
    final T defaltValue,
    final Getter<T> getter
  ) throws SQLException {
    final String targetUrl = parseURL(url);
    if (targetUrl == null) {
      return defaltValue;
    }
    final Driver driver = DriverManager.getDriver(targetUrl);
    if (driver == null) {
      return defaltValue;
    }
    return getter.get(driver, targetUrl);
  }

  /**
   * @param url jdbc:spylog:<driverClassName>/<targetUrl>
   * @return targetUrl
   * @throws SQLException
   */
  private static String parseURL(final String url) throws SQLException {
    if (!url.startsWith(JDBC_PREFIX) ) {
      return null;
    }
    final String subUrl = url.substring(JDBC_PREFIX.length() );
    final int index = subUrl.indexOf('/');
    if (index == -1) {
      return null;
    }
    if (index > 0) {
      try {
        Class.forName(subUrl.substring(0, index) );
      } catch(ClassNotFoundException e) {
        return null;
      }
    }
    return subUrl.substring(index + 1);
  }

  private interface Getter<T> {
    T get(Driver driver, String targetUrl) throws SQLException;
  }

  protected static class Invoker implements InvocationHandler {

    @SuppressWarnings("unchecked")
    public static <T> T newProxy(
        final String targetUrl, final T target, final Class<?> intf
    ) {
      return (T)Proxy.newProxyInstance(
          target.getClass().getClassLoader(),
          new Class<?>[] { intf },
          new Invoker(targetUrl, target) );
    }

    private final String targetUrl;

    private final Object target;

    public Invoker(final String targetUrl, final Object target) {
      this.targetUrl = targetUrl;
      this.target = target;
    }

    @Override
    public Object invoke(
        final Object proxy,
        final Method method,
        final Object[] args
    ) throws Throwable {
      final String methodName = method.getName();
      handler.enter(targetUrl, target, methodName, args);
      try {
        final Object result = method.invoke(target, args);
        handler.result(targetUrl, target, methodName, result);
        if (result == null) {
          return result;
        } else if (!method.getReturnType().isInterface() ) {
          return result;
        } else if (Proxy.isProxyClass(result.getClass() ) &&
            Invoker.class.isAssignableFrom(
                Proxy.getInvocationHandler(result).getClass() ) ) {
          // already wrapped.
          return result;
        } else {
          return Invoker.newProxy(
              targetUrl, result, method.getReturnType() );
        }
      } catch(InvocationTargetException e) {
        handler.throwable(targetUrl, target, methodName, e.getCause() );
        throw e;
      } catch(Throwable t) {
        handler.throwable(targetUrl, target, methodName, t);
        throw t;
      } finally {
        handler.exit(targetUrl, target, methodName);
      }
    }
  }
}
