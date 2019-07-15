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
        String sql = String.format("SELECT authState FROM main where " +
                "login = '%s' and password = '%s'", login, pass);
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            if(rs.getString(1).equals("false")){
                sql = String.format("UPDATE main  SET authState = '%s' where login = '%s'", "true", login);
                stmt.executeUpdate(sql);
                return login;
            } else {
                return null;
            }
        }
        return null;
    }

    public static boolean closeConnectByLogin(String login) throws SQLException {
        String sql = String.format("SELECT authState FROM MAIN WHERE login = '%s'", login);
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            sql = String.format("UPDATE main  SET authState = '%s' where login = '%s'", "false", login);
            stmt.executeUpdate(sql);
            return true;
        } else {
            return false;
        }
    }


}
