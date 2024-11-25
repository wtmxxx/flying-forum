package com.atcumt.gateway;

import com.atcumt.common.config.CommonFeignConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = {CommonFeignConfiguration.class}, basePackages = {"com.atcumt.gateway", "com.atcumt.common.api.client"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
