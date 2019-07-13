public class AuthResult extends AbstractMessage {
    private String result;

    public AuthResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
