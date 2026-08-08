package com.noahgeerts.progress;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

  @GetMapping("/public/hello")
  public String publicHello() {
    return "Hello Everyone";
  }

  @GetMapping("/private/hello")
  public String privateHello(@AuthenticationPrincipal Jwt jwt) {
    String sub = jwt.getClaim("sub");
    return "Hello Authenticated User: " + sub;
  }

}
