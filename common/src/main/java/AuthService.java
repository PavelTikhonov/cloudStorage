import java.sql.*;

public class AuthService {

    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:userDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String checkLoginAndPass(String login, String pass) throws SQLException {
        String sql = String.format("SELECT nickname FROM main where " +
                "login = '%s' and password = '%s'", login, pass);
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            return login;
        }
        return null;
    }

    public static String getBlacklistByNick(String nick) throws SQLException {
        String sql = String.format("SELECT blacklist FROM main where nickname = '%s'", nick);
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            return rs.getString(1);
        }
        return null;
    }

    public static boolean setBlacklistByNick(String nick, String blacklist) throws SQLException {
        String sql = String.format("SELECT blacklist FROM main where nickname = '%s'", nick);
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            sql = String.format("UPDATE main  SET blacklist = '%s' where nickname = '%s'", blacklist, nick);
            stmt.executeUpdate(sql);
            return true;
        } else {
            return false;
        }
    }


}
