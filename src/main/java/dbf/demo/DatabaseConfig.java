package dbf.demo;

import lombok.Getter;

@Getter
public class DatabaseConfig {

    private final String url;
    private final String userName;
    private final String password;

    public DatabaseConfig(String url, String userName, String password){
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

}
