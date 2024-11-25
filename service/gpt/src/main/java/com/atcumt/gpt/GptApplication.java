package com.atcumt.gpt;

import com.atcumt.common.config.CommonFeignConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableWebSocket
@EnableFeignClients(defaultConfiguration = {CommonFeignConfiguration.class}, basePackages = {"com.atcumt.gpt", "com.atcumt.common.api.client"})
@EnableDiscoveryClient
public class GptApplication {

    public static void main(String[] args) {
        SpringApplication.run(GptApplication.class, args);
    }

}
