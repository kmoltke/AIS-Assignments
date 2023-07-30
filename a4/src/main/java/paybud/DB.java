package paybud;

/*
 * Interface to the PayBud database.
 */

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DB {

    private static final String URL = "jdbc:sqlite:db/paybud_salt.db"; // database we are connecting to.
    
    public static boolean create( final String email, final String password ) {

        byte[] salt = genSalt();
        String hashStr = null;
        try {
            hashStr = Base64.getEncoder().encodeToString(hashPwd(password, salt));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        String saltStr = Base64.getEncoder().encodeToString(salt);

        final String iu = "INSERT INTO users VALUES ('" + email + "', '" + saltStr + "', '" + hashStr + "');";
        final String ia = "INSERT INTO accounts VALUES ('" + email + "', '0'); ";
        final String it = "INSERT INTO tokens (email) VALUES ('" + email + "');";
        try {
            Connection c; Statement s;
            c = DriverManager.getConnection(URL);
            c.setAutoCommit(false); // enter transaction mode
            s = c.createStatement();
            s.executeUpdate(iu);
            s = c.createStatement();
            s.executeUpdate(ia);
            s = c.createStatement();
            s.executeUpdate(it);
            c.commit();             // commit transaction
            c.setAutoCommit(true);  // exit transaction mode
            c.close();
            return true;
        } catch ( Exception e ) {}
        return false; // exception occurred; malformed SQL query?
    }

    private static byte[] genSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[32];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPwd(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        return factory.generateSecret(spec).getEncoded();
    }


    public static Optional<String> createToken(final String email) {
        String q = "UPDATE tokens SET token = ?, time = ? WHERE email = ?";
        try (Connection c = DriverManager.getConnection(URL)){
            String token = generateToken();
            c.setAutoCommit(false);
            PreparedStatement ps = c.prepareStatement(q);
            ps.setString(1, token);
            ps.setString(2, currentTime());
            ps.setString(3, email);
            ps.executeUpdate();
            c.commit();
            c.setAutoCommit(true);
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
        final String q = "SELECT * FROM users WHERE email=?";
        try (Connection c = DriverManager.getConnection(URL)) {
            String u; String hashword; String saltStr;
            PreparedStatement ps = c.prepareStatement(q);
            ps.setString(1, email);
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                hashword = r.getString("hashword");
                saltStr = r.getString("salt");
                byte[] salt = Base64.getDecoder().decode(saltStr);
                byte[] hash = Base64.getDecoder().decode(hashword);
                if (Arrays.equals(hash, hashPwd(password, salt))) {
                    u = r.getString("email");
                } else {
                    u = null;
                }
            } else {
                u = null;
            }
            return Optional.ofNullable(u);
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
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
