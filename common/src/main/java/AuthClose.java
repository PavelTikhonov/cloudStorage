public class AuthClose extends AbstractMessage {
    private String login;

    public AuthClose(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
