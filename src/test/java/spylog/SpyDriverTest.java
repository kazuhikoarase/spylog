package spylog;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

import spylog.SpyDriver.Invoker;

public class SpyDriverTest {

  public interface Intf { String getName(); };
  public static class Impl implements Intf {
    @Override
    public String getName() {
      return "hello";
    }
  }

  @Test
  public void test() throws Exception {

    Intf o = new Impl();
    Intf proxy = Invoker.newProxy("hello", o, Intf.class);
    Assert.assertTrue(Invoker.class.isAssignableFrom(
        Proxy.getInvocationHandler(proxy).getClass() ) );

    String driverClassName = "spylog.SpyDriver";
    String url = "jdbc:spylog:org.hsqldb.jdbcDriver/jdbc:hsqldb:mem:spydb";
    String user = "SA";
    String pass = "";

    Class.forName(driverClassName);
    Connection conn = DriverManager.getConnection(url, user, pass);
    try {

      executeUpdate(conn, "create table TEST \n\n    ( K char(1) )");
      conn.commit();

      executeUpdate(conn, "insert into TEST values ('a')");
      executeUpdate(conn, "insert into TEST values ('b')");
      executeUpdate(conn, "insert into TEST values ('c')");
      conn.commit();

    } finally {
      conn.close();
    }
  }

  protected void executeUpdate(
      Connection conn, String sql) throws Exception {
    Statement stmt = conn.createStatement();
    try {
      stmt.executeUpdate(sql);
      conn.commit();
    } finally {
      stmt.close();
    }
  }
}
