package com.atcumt.apidoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@SpringBootApplication
@RestController
public class ApidocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApidocApplication.class, args);
    }

    @GetMapping("/")
    public RedirectView redirectToDoc() {
        return new RedirectView("/doc.html"); // 重定向到 /doc 路径
    }

}
