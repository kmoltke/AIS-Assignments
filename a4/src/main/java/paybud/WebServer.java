package paybud;

/*
 * Web Server for the PayBud payment system.
 */

// New imports
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.*;
import java.security.cert.CertificateException;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import org.json.simple.JSONObject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.passay.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class WebServer {
    private static final String  HOSTNAME = "localhost";
    private static final int     PORT     = 5000;
    private static final int     BACKLOG  = -1;
    private static final Charset CHARSET  = StandardCharsets.UTF_8;

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static SecretKey hmacKey;

    private static final Logger log = LoggerFactory.getLogger("PayBud");
    private static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(final String... args)
            throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException {

        String disalg;
        String passwd;
        KeyStore keysto;
        KeyManagerFactory keyman;
        TrustManagerFactory truman;
        SSLContext sslctx;
        HttpsConfigurator config;
        HttpsServer server;
        Executor thpool;

        // Java Secure Sockets Extension (JSSE), Configuration
        disalg = java.security.Security.getProperty("jdk.tls.disabledAlgorithms");
        disalg = cslRemove(disalg, "SSLv3");   // Re-enabled to support legacy browsers. - PayBud dev
        disalg = cslRemove(disalg, "TLSv1");   // Re-enabled to support legacy browsers. - PayBud dev
        disalg = cslRemove(disalg, "TLSv1.1"); // Re-enabled to support legacy browsers. - PayBud dev
//        java.security.Security.setProperty("jdk.tls.disabledAlgorithms", disalg);
//        java.security.Security.setProperty("jdk.tls");
//        System.setProperty("https.protocols", "TLSv1.2");

        java.security.Security.setProperty("jdk.tls.client.protocols", "TLSv1.2");

        // Server's private key & certificate
        passwd = "password";
        keysto = KeyStore.getInstance("PKCS12");
        keyman = KeyManagerFactory.getInstance("SunX509");
        truman = TrustManagerFactory.getInstance("SunX509");
        keysto.load(new FileInputStream("paybud.p12"), passwd.toCharArray());
        keyman.init(keysto, passwd.toCharArray());
        truman.init(keysto);

        String hmacKeyAlias = "HMACkey";
        KeyStore.SecretKeyEntry secretKey = null;
        try {
            secretKey = (KeyStore.SecretKeyEntry) keysto.getEntry(hmacKeyAlias, new KeyStore.PasswordProtection(passwd.toCharArray()));
        } catch (UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        }
        hmacKey = secretKey.getSecretKey();

        // Server's SSL configuration
        sslctx = SSLContext.getInstance("TLSv1.2");
        sslctx.init(keyman.getKeyManagers(), truman.getTrustManagers(), null);

        // Server creation
        server = HttpsServer.create(new InetSocketAddress(PORT), BACKLOG);
        server.setHttpsConfigurator(new HttpsConfigurator(sslctx));

        // Threadpool
        thpool = Executors.newFixedThreadPool(16);
        server.setExecutor(thpool);

        // other
        server.createContext("/",                 io -> { other(io); });
        // pages (login, menu, etc.)
        server.createContext("/login",            io -> { loginPage(io); });
        server.createContext("/menu",             io -> { menuPage(io); });
        server.createContext("/send",             io -> { sendPage(io); });
        server.createContext("/deposit",          io -> { depositPage(io); });
        server.createContext("/withdraw",         io -> { withdrawPage(io); });
        // API
        server.createContext("/api/create",       io -> { create(io); });
        server.createContext("/api/forgot",       io -> { forgot(io); });
        server.createContext("/api/login",        io -> { login(io); });
        server.createContext("/api/balance",      io -> { balance(io); });
        server.createContext("/api/send",         io -> { send(io); });
        server.createContext("/api/deposit",      io -> { deposit(io); });
        server.createContext("/api/withdraw",     io -> { withdraw(io); });
        server.createContext("/api/logout",       io -> { logout(io); });
        server.createContext("/api/genToken", io -> {genToken(io);});
        server.createContext("/api/auth", io -> {authToken(io);});
        // files
        server.createContext("/style.css",        io -> { respond(io, 200, "text/css", readFile("static/style.css")); });
        server.createContext("/favicon.ico",      io -> { respond(io, 200, "image/png", readFile("static/favicon.ico")); });
        server.createContext("/paybud.png",       io -> { respond(io, 200, "image/png", readFile("static/paybud.png")); });
        server.createContext("/api.js",           io -> { respond(io, 200, "application/javascript", readFile("static/api.js")); });
        server.createContext("/login/code.js",    io -> { respond(io, 200, "application/javascript", readFile("static/login/code.js")); });
        server.createContext("/menu/code.js",     io -> { respond(io, 200, "application/javascript", readFile("static/menu/code.js")); });
        server.createContext("/send/code.js",     io -> { respond(io, 200, "application/javascript", readFile("static/send/code.js")); });
        server.createContext("/deposit/code.js",  io -> { respond(io, 200, "application/javascript", readFile("static/deposit/code.js")); });
        server.createContext("/withdraw/code.js", io -> { respond(io, 200, "application/javascript", readFile("static/withdraw/code.js")); });

        log.info(dateformat.format(new Date()) + " " + "Starting web server.");

        server.start();
    }

    private static void other(final HttpExchange io){
        redirect(io, "/login");
    }
    private static void redirect(final HttpExchange io, final String location){
        io.getResponseHeaders().set("Location", location);
        try {
            io.sendResponseHeaders(302,0);
        } catch ( Exception e ) {} finally { io.close(); }
    }

    /*
     * Pages
     */
    private static void loginPage(final HttpExchange io) {
        // if already logged in, redirect to menu page.
        if ( authenticated(io) ){
            redirect(io, "/menu");
            return;
        }
        // not logged in. present login page.
        respond(io, 200, "text/html", readFile("static/login/index.html"));
    }

    private static void menuPage(final HttpExchange io) {
        // if not logged in, redirect to login page.
        if ( ! authenticated(io) ){
            redirect(io, "/login");
            return;
        }
        // logged in. present menu page.
        respond(io, 200, "text/html", readFile("static/menu/index.html"));
    }

    private static void sendPage(final HttpExchange io) {
        // if not logged in, redirect to login page.
        if ( ! authenticated(io) ){
            redirect(io, "/login");
            return;
        }
        // logged in. present send page.
        respond(io, 200, "text/html", readFile("static/send/index.html"));
    }

    private static void depositPage(final HttpExchange io) {
        // if not logged in, redirect to login page.
        if ( ! authenticated(io) ){
            redirect(io, "/login");
            return;
        }
        // logged in. present deposit page.
        respond(io, 200, "text/html", readFile("static/deposit/index.html"));
    }

    private static void withdrawPage(final HttpExchange io) {
        // if not logged in, redirect to login page.
        if ( ! authenticated(io) ){
            redirect(io, "/login");
            return;
        }
        // logged in. present withdraw page.
        respond(io, 200, "text/html", readFile("static/withdraw/index.html"));
    }

    /*
     * API operations
     */
    private static void genToken(final HttpExchange io) {
        if ( authenticated(io) ){
            respond(io, 409, "application/json", json("Already logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final Optional<String> result = DB.createToken(qMap.get("email"));
        if (!result.isPresent()) {
            respond(io, 400, "application/json", json("Syntax error in token creation query."));
            log(io, qMap.get("email") + " failed generating token.");
            return;
        }

        final String subject = "PayBud token";
        final Function<String,String> body = (token) -> "Hi!\n\nYour PayBud Token is \"" + token + "\" (w/o quotes).\n\nCheers!\nPayBud";

        final boolean emailSuccess = EM.send(qMap.get("email"), subject, body.apply(result.get()));

        if ( ! emailSuccess ){
            respond(io, 401, "application/json", json("Error sending password email to " + qMap.get("email")));
            return;
        }
        respond(io, 200, "application/json", json("Token successfully e-mailed to " + qMap.get("email")));
        log(io, "token sent to " + qMap.get("email"));
    }

    private static void authToken(final HttpExchange io) {
        if ( authenticated(io) ){
            respond(io, 409, "application/json", json("Already logged in."));
            return;
        }

        final Map<String, String> qMap = queryMap(io);
        final String email = qMap.get("email");
        final String token = qMap.get("token");

        Optional<ArrayList<String>> result = DB.getToken(email, token);
        if (!result.isPresent()) {
            respond(io, 401, "application/json", json("Token is invalid."));
            log(io, email + " provided invalid token.");
        } else if (tokenExpired(result.get().get(1))) {
            respond(io, 419, "application/json", json("Token is timed out."));
            log(io, email + " token is timed out.");
        } else {
            authenticate(io, email);
            respond(io, 200, "application/json", json("Login authenticated."));
            log(io, email + " was successfully authenticated.");
        }
    }

    private static boolean tokenExpired(final String tokenTime) {
        // Parse the token creation time string into a LocalDateTime object
        LocalDateTime parsedTokenTime = LocalDateTime.parse(tokenTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        // Get the current time as a LocalDateTime object
        LocalDateTime currentTime = LocalDateTime.now();

        // Calculate the difference between the current time and the token creation time
        long minutesDifference = parsedTokenTime.until(currentTime, ChronoUnit.MINUTES);

        // Change expiration time here:
        return minutesDifference > 1;
    }

    private static void create(final HttpExchange io){
        if ( authenticated(io) ){
            respond(io, 409, "application/json", json("Already logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final Optional<String> result = DB.user(qMap.get("email"));

        final boolean userSuccess = (result != null);
        if ( ! userSuccess ){
            respond(io, 400, "application/json", json("Syntax error in user existence query."));
            return;
        }

        final boolean userExists = result.isPresent();
        if ( userExists ){
            respond(io, 403, "application/json", json("User already exists."));
            return;
        }

        PasswordValidator validator = getPasswordValidator();
        RuleResult ruleResult = validator.validate(new PasswordData(qMap.get("password")));

        if (!ruleResult.isValid()) {
            respond(io, 400, "application/json", json("Password is not strong enough"));
            return;
        }

        final boolean createSuccess = DB.create(qMap.get("email"), qMap.get("password"));
        if ( ! createSuccess ){
            respond(io, 400, "application/json", json("Syntax error in user creation query."));
            return;
        }

        respond(io, 200, "application/json", json("User created successfully."));
    }

    private static void forgot(final HttpExchange io){
        if ( authenticated(io) ){
            respond(io, 409, "application/json", json("Already logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final Optional<String> result = DB.password(qMap.get("email"));

        final boolean userSuccess = (result != null);
        if ( ! userSuccess ){
            respond(io, 400, "application/json", json("Syntax error in user existence query."));
            return;
        }

        final boolean userExists = result.isPresent();
        if ( ! userExists ){
            respond(io, 401, "application/json", json("Email is invalid."));
            return;
        }

        final String subject = "PayBud password";
        final Function<String,String> body = (pw) -> "Hi!\n\nYour PayBud Password is \"" + pw + "\" (w/o quotes).\n\nCheers!\nPayBud";

        final boolean emailSuccess = EM.send(qMap.get("email"), subject, body.apply(result.get()));

        if ( ! emailSuccess ){
            respond(io, 401, "application/json", json("Error sending password email to " + qMap.get("email")));
            return;
        }
        respond(io, 200, "application/json", json("Password successfully e-mailed to " + qMap.get("email")));
    }

    private static void login(final HttpExchange io){
        if ( authenticated(io) ){
            respond(io, 409, "application/json", json("Already logged in."));
            return;
        }

        final Map<String,String>  qMap = queryMap(io);
        final String email = qMap.get("email");
        final String password = qMap.get("password");
        final Optional<String> result = DB.login(email, password);

        final boolean loginSuccess = (result != null);
        if ( ! loginSuccess ){
            respond(io, 400, "application/json", json("Syntax error in the request."));
            log(io, "has a syntax error.");
            return;
        }

        final boolean userExists = result.isPresent();
        if ( ! userExists ){
            respond(io, 401, "application/json", json("Email and password are invalid."));
            log(io, email + " provided invalid password.");
        } else {
            respond(io, 200, "application/json", json("Login successful."));
            log(io, email + " logged in.");
        }
    }

    private static void balance(final HttpExchange io){
        if ( ! authenticated(io) ){
            respond(io, 409, "application/json", json("Not logged in."));
            return;
        }

        final Optional<String> result = DB.balance(getEmail(io));

        final boolean balanceSuccess = (result != null);
        if ( ! balanceSuccess ){
            respond(io, 400, "application/json", json("Syntax error in the request."));
            return;
        }

        final boolean balanceExists = result.isPresent();
        if ( ! balanceExists ){
            respond(io, 401, "application/json", json("Email has no account."));
        } else {
            respond(io, 200, "application/json", json("Balance successful.", result.get()));
        }
    }

    private static void send(final HttpExchange io){
        if ( ! authenticated(io) ){
            respond(io, 409, "application/json", json("Not logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final String amount = qMap.get("amount");

        if ( ! integer(amount) ) {
            respond(io, 400, "application/json", json("Not an integer amount."));
            return;
        }
        if ( ! positive(amount) ) {
            respond(io, 400, "application/json", json("Not a positive integer amount."));
            return;
        }

        final Optional<String> result = DB.user(qMap.get("to"));

        final boolean userSuccess = (result != null);
        if( ! userSuccess ){
            respond(io, 400, "application/json", json("Syntax error in 'to'."));
            return;
        }

        final boolean userExists = result.isPresent();
        if ( ! userExists ){
            respond(io, 403, "application/json", json("'to' user does not exist."));
            return;
        }

        final boolean sendSuccess = DB.send(getEmail(io), qMap.get("to"), amount);
        if ( ! sendSuccess ){
            respond(io, 400, "application/json", json("Syntax error in the request."));
            return;
        }

        respond(io, 200, "application/json", json("Send successful."));
    }

    private static void deposit(final HttpExchange io){
        if ( ! authenticated(io) ){
            respond(io, 409, "application/json", json("Not logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final String amount = qMap.get("amount");

        if ( ! integer(amount) ) {
            respond(io, 400, "application/json", json("Not an integer amount."));
            return;
        }
        if ( ! positive(amount) ) {
            respond(io, 400, "application/json", json("Not a positive integer amount."));
            return;
        }

        final boolean withdrawSuccess = CC.withdraw(qMap.get("cardnumber"), amount);
        if ( ! withdrawSuccess ){
            respond(io, 400, "application/json", json("Credit card withdrawal request rejected."));
            return;
        }

        final boolean depositSuccess = DB.deposit(getEmail(io), amount);
        if ( ! depositSuccess ){
            respond(io, 400, "application/json", json("Syntax error in the request."));
            return;
        }

        respond(io, 200, "application/json", json("Deposit successful."));
    }

    private static void withdraw(final HttpExchange io){
        if ( ! authenticated(io) ){
            respond(io, 409, "application/json", json("Not logged in."));
            return;
        }

        final Map<String,String> qMap = queryMap(io);
        final String amount = qMap.get("amount");

        if ( ! integer(amount) ) {
            respond(io, 400, "application/json", json("Not an integer amount."));
            return;
        }
        if ( ! positive(amount) ) {
            respond(io, 400, "application/json", json("Not a positive integer amount."));
            return;
        }

        final boolean withdrawSuccess = DB.withdraw(getEmail(io), amount);
        if ( ! withdrawSuccess ){
            respond(io, 400, "application/json", json("Syntax error in the request."));
            return;
        }

        final boolean depositSuccess = CC.deposit(qMap.get("cardnumber"), amount);
        if ( ! depositSuccess ){
            final boolean refundSuccess = DB.deposit(getEmail(io), amount);
            if ( ! refundSuccess ) {
                respond(io, 400, "application/json", json("Money disappeared! credit card deposit request rejected, and account refund failed."));
                return;
            } else {
                respond(io, 400, "application/json", json("Credit card deposit request rejected."));
                return;
            }
        }

        respond(io, 200, "application/json", json("Withdraw successful."));
    }

    private static void logout(final HttpExchange io) {
        if ( ! authenticated(io) ){
            respond(io, 409, "application/json", json("Not logged in."));
            return;
        }

        deauthenticate(io);
        respond(io, 200, "application/json", json("Logout successful."));
    }

    /*
     * Authentication
     */
    private static void authenticate(final HttpExchange io, final String email) {
        createCookie(io, email);
    }
    private static void deauthenticate(final HttpExchange io) {
        deleteCookie(io);
    }
    private static boolean authenticated(final HttpExchange io) {
        if ( hasCookie(io) && goodCookie(io) ) {
            return true;
        } else {
            deleteCookie(io);
            return false;
        }
    }

    /*
     * Cookie operations
     */
    private static void createCookie(final HttpExchange io, final String email){
        List<String> l = new ArrayList<String>();
        l.add("email=" + email + "; path=/");
        l.add("hash=" + calculateHMAC(email, hmacKey) + "; path=/");
        io.getResponseHeaders().put("Set-Cookie", l);
    }

    public static String calculateHMAC(String data, SecretKey key) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA512);
            mac.init(key);
            return Base64.getUrlEncoder().encodeToString(mac.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteCookie(final HttpExchange io){
        List<String> l = new ArrayList<String>();
        l.add("email=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/");
        l.add("hash=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/");
        io.getResponseHeaders().put("Set-Cookie", l);
    }
    private static boolean hasCookie(final HttpExchange io){
        return io.getRequestHeaders().containsKey("Cookie");
    }
    private static boolean goodCookie(final HttpExchange io){
        final Optional<String> result = DB.user(getEmail(io));

        final boolean userSuccess = (result != null);
        if ( ! userSuccess ){
            return false; // SQL query failed
        }

        final boolean userExists = result.isPresent();
        if ( ! userExists ){
            return false; // user given in cookie does not exist in PayBud
        }
        final byte[] hash = Base64.getUrlDecoder().decode(getHash(io));
        final byte[] calc = Base64.getUrlDecoder().decode(calculateHMAC(getEmail(io), hmacKey));

        return Arrays.equals(hash, calc); // hash given in cookie failed integrity check
    }
    private static String getEmail(final HttpExchange io){
        String[] pairs = io.getRequestHeaders().get("Cookie").get(0).split(" *; *");
        if ( pairs.length < 1 ) { return ""; }
        return pairs[0].split("=", -1)[1];
    }
    private static String getHash(final HttpExchange io){
        String[] pairs = io.getRequestHeaders().get("Cookie").get(0).split(" *; *");
        if ( pairs.length < 2 ) { return ""; }
        return pairs[1].split("=", -1)[1];
    }

    /*
     * Integer operations
     */
    private static boolean integer(final String amount){
        try {
            Integer.decode(amount);
            return true;
        } catch ( Exception e ){}
        return false;
    }
    private static boolean positive(final String amount){
        try {
            int i = Integer.decode(amount).intValue();
            return ( i > 0 );
        } catch ( Exception e ){}
        return false;
    }

    /*
     * JSON operations
     */
    @SuppressWarnings("unchecked") //JSONObject uses HashMap w/o specifying types.
    private static byte[] json(final String text) {
        JSONObject o = new JSONObject();
        o.put("text", text);
        try {
            return o.toString().getBytes(CHARSET.name());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @SuppressWarnings("unchecked") //JSONObject uses HashMap w/o specifying types.
    private static byte[] json(final String text, final String balance) {
        JSONObject o = new JSONObject();
        o.put("text", text);
        o.put("balance", balance);
        try {
            return o.toString().getBytes(CHARSET.name());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Response formatting
     */
    private static void respond(final HttpExchange io, final int response_code, final String mime, final byte[] response){
        try {
            io.getResponseHeaders().set("Content-Type", String.format(mime + "; charset=%s", CHARSET.name()));
            io.sendResponseHeaders(response_code, response.length);
            io.getResponseBody().write(response);
        } catch ( Exception e ) {} finally { io.close(); }
    }
    public static byte[] readFile(final String path){
        try{
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    /*
     * URI operations
     */
    private static String path(final HttpExchange io){
        return io.getRequestURI().getRawPath();
    }
    private static String query(final HttpExchange io){
        return io.getRequestURI().getRawQuery();
    }
    private static Map<String, String> queryMap(final HttpExchange io) {
        final Map<String, String> qMap = new HashMap<>();
        final String qRaw = query(io);
        if (qRaw != null) {
            for ( final String pair : qRaw.split("[&;]", -1) ) {
                final String[] q   = pair.split("=", 2);
                final String   key = decodeURL(q[0]);
                final String   val = q.length > 1 ? decodeURL(q[1]) : null;
                qMap.put(key, val);
            }
        }
        return qMap;
    }
    private static String decodeURL(final String url) {
        try {
            return URLDecoder.decode(url, CHARSET.name());
        } catch ( Exception e ) {}
        return "";
    }

    /*
     * Comma-Separated List (CSL) string operations
     */
    private static String cslAdd(final String csl, final String s){
        List<String> l = new ArrayList<String>( Arrays.asList(csl.split(",\\s*")) );
        if ( ! l.contains( s ) ) {
            l.add(s);
        }
        return String.join(", ", l);
    }
    private static String cslRemove(final String csl, final String s){
        List<String> l = new ArrayList<String>( Arrays.asList(csl.split(",\\s*")) );
        l.remove(s);
        return String.join(", ", l);
    }

    // HELPERS
    private static void log(HttpExchange io, String msg) {
        log.info(io.getRemoteAddress().toString() + " - [" + dateformat.format(new Date()) + "]: " + msg);
    }

    private static PasswordValidator getPasswordValidator() {
        PasswordValidator validator = new PasswordValidator(
                new LengthRule(8, 16),                                      // Minimum and maximum password length
                new CharacterRule(EnglishCharacterData.UpperCase, 1),  // Require at least one uppercase letter
                new CharacterRule(EnglishCharacterData.LowerCase, 1),  // Require at least one lowercase letter
                new CharacterRule(EnglishCharacterData.Digit, 1),      // Require at least one digit
                new CharacterRule(EnglishCharacterData.Special, 1)     // Require at least one special character
        );

        return validator;
    }
}
