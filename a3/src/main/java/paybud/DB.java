package paybud;

/*
 * Interface to the PayBud database.
 */

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.sql.PreparedStatement;
import java.util.Random;

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

    public static Optional<String> createToken(final String email) {
        String q = "UPDATE tokens SET token = ?, time = ? WHERE email = ?";
//        String q = "UPDATE tokens SET token = ? WHERE email = ?";
        try (Connection c = DriverManager.getConnection(URL)){
            String token = generateToken();
            c.setAutoCommit(false);
            PreparedStatement ps = c.prepareStatement(q);
            ps.setString(1, token);
            ps.setString(2, currentTime());
            ps.setString(3, email);
            ps.executeUpdate();
            c.commit();
            return Optional.of(token);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<ArrayList<String>> getToken(final String email, final String token) {
        String q = "SELECT * FROM tokens WHERE email = ? AND token = ?";
        try (Connection c = DriverManager.getConnection(URL)) {
            ArrayList<String> result = new ArrayList<>();
            PreparedStatement ps = c.prepareStatement(q);
            ps.setString(1, email);
            ps.setString(2, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.add(rs.getString("token"));
                result.add(rs.getString("time"));
            } else {
                return Optional.empty();
            }
            return Optional.of(result);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<String> login( final String email, final String password ) {
        final String q = "SELECT * FROM users WHERE email=? AND password=?";
        try {
            Connection c; ResultSet r; String u;
            c = DriverManager.getConnection(URL);
            PreparedStatement ps = c.prepareStatement(q);
            ps.setString(1, email);
            ps.setString(2, password);
            r = ps.executeQuery();

            //s = c.createStatement();
            //r = s.executeQuery(q);
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

    private static String generateToken() {
        // Define the length of the random token
        int tokenLength = 10;

        // Define the characters to be used in the random token
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        // Create a StringBuilder to store the random token
        StringBuilder randomToken = new StringBuilder();

        // Create a random number generator
        Random random = new Random();

        // Generate the random token by appending random characters
        for (int i = 0; i < tokenLength; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomToken.append(randomChar);
        }

        return randomToken.toString();
    }

    private static String currentTime() {
        // Define the desired date-time format
        String formatPattern = "yyyy-MM-dd HH:mm:ss.SSS";

        // Get the current date-time
        LocalDateTime currentTime = LocalDateTime.now();

        // Create a DateTimeFormatter with the desired format pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);

        return currentTime.format(formatter);
    }
}
