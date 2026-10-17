package dfb.demo;

import dfb.di.Validate;
import lombok.Getter;

@Getter
public class DatabaseConfig {

    private final String url;
    private final String userName;
    private final String password;

    public DatabaseConfig(String url, String userName, String password) {
        this.url = Validate.trimmedNonBlank(url, "url");
        this.userName = Validate.trimmedNonBlank(userName, "userName");
        this.password = Validate.trimmedNonBlank(password, "password");
    }
}
