package me.zinch.itmo.mts.eis;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "eis.killbill")
public class KillBillProperties {

    private String url = "http://localhost:8080";
    private String apiKey = "mts";
    private String apiSecret = "mts-secret";
    private String username = "admin";
    private String password = "password";
    private Map<String, String> plans = new HashMap<>();
}
