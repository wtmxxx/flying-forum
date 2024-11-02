package com.atcumt.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiConfiguration {
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("矿大论坛")
                        .description("矿大论坛 API文档")
                        .contact(
                                new Contact().name("Wotemo").email("wotemo@qq.com")
                        )
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0").url("https://kxz.atcumt.com")))
//                .externalDocs(new ExternalDocumentation()
//                        .description("SpringShop Wiki Documentation")
//                        .url("https://springshop.wiki.github.org/docs"))
                ;
    }
}
