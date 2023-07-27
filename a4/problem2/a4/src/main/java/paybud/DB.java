package paybud;

/*
 * Interface to the PayBud database.
 */

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Optional;

public class DB {

    private static final String URL = "jdbc:sqlite:db/paybud.db"; // database we are connecting to.
    
    public static boolean create( final String email, final String password ) {
        final String iu = "INSERT INTO users VALUES ('" + email + "', '" + password + "');";
        final String ia = "INSERT INTO accounts VALUES ('" + email + "', '0'); ";
        try {
            Connection c; Statement s;
            c = DriverManager.getConnection(URL);
            c.setAutoCommit(false); // enter transaction mode
            s = c.createStatement();
            s.executeUpdate(iu);
            s = c.createStatement();
            s.executeUpdate(ia);
            c.commit();             // commit transaction
            c.setAutoCommit(true);  // exit transaction mode
            c.close();
            return true;
        } catch ( Exception e ) {}
        return false; // exception occurred; malformed SQL query?
    }

    public static Optional<String> login( final String email, final String password ) {
        final String q = "SELECT * FROM users WHERE email='" + email + "' AND password='" + password + "'";
        try {
            Connection c; Statement s; ResultSet r; String u;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            r = s.executeQuery(q);
            if ( r.next() ){ // true iff result set non-empty, implying email-password combination found.
                u = r.getString("email");
            } else {
                u = null;
            }
            c.close();
            return Optional.ofNullable(u); // empty iff u = null
        } catch ( Exception e ) {}
        return null; // exception occurred; malformed SQL query?
    }
    
    public static Optional<String> user( final String email ) {
        final String q = "SELECT * FROM users WHERE email='" + email + "'";
        try {
            Connection c; Statement s; ResultSet r; String u;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            r = s.executeQuery(q);
            if ( r.next() ){ // true iff result set non-empty, implying email found.
                u = r.getString("email");
            } else {
                u = null;
            }
            c.close();
            return Optional.ofNullable(u); // empty iff u = null
        } catch ( Exception e ) {}
        return null; // exception occurred; malformed SQL query?
    }
    
    public static Optional<String> password( final String email ) {
        final String q = "SELECT * FROM users WHERE email='" + email + "'";
        try {
            Connection c; Statement s; ResultSet r; String u;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            r = s.executeQuery(q);
            if ( r.next() ){ // true iff result set non-empty, implying email found.
                u = r.getString("password");
            } else {
                u = null;
            }
            c.close();
            return Optional.ofNullable(u); // empty iff u = null
        } catch ( Exception e ) {}
        return null; // exception occurred; malformed SQL query?
    }
    
    public static Optional<String> balance( final String email ) {
        final String q = "SELECT balance FROM accounts WHERE email='" + email + "'";
        try {
            Connection c; Statement s; ResultSet r; String b;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            r = s.executeQuery(q);
            if ( r.next() ){ // true iff user has an account
                b = r.getString("balance"); // null iff no rows returned
            } else {
                b = null;
            }
            c.close();
            return Optional.ofNullable(b);
        } catch ( Exception e ) {}
        return null; // exception occurred; malformed SQL query?
    }
    
    public static boolean send( final String email, final String to, final String amount ) {
        final String uf = "UPDATE accounts SET balance = balance - " + amount + " WHERE email = '" + email + "';";
        final String ut = "UPDATE accounts SET balance = balance + " + amount + " WHERE email = '" + to + "';";
        try {
            Connection c; Statement s;
            c = DriverManager.getConnection(URL);
            c.setAutoCommit(false);
            s = c.createStatement();
            s.executeUpdate(uf);
            s = c.createStatement();
            s.executeUpdate(ut);
            c.commit();
            c.setAutoCommit(true);
            c.close();
            return true;
        } catch ( Exception e ) {}
        return false; // exception occurred; malformed SQL query?
    }
    
    public static boolean deposit( final String email, final String amount ) {
        final String u = "UPDATE accounts SET balance = balance + " + amount + " WHERE email = '" + email + "';";
        try {
            Connection c; Statement s;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            s.executeUpdate(u);
            c.close();
            return true;
        } catch ( Exception e ) {}
        return false; // exception occurred; malformed SQL query?
    }
    
    public static boolean withdraw( final String email, final String amount ) {
        final String u = "UPDATE accounts SET balance = balance - " + amount + " WHERE email = '" + email + "';";
        try {
            Connection c; Statement s;
            c = DriverManager.getConnection(URL);
            s = c.createStatement();
            s.executeUpdate(u);
            c.close();
            return true;
        } catch ( Exception e ) {}
        return false; // exception occurred; malformed SQL query?
    }
}
