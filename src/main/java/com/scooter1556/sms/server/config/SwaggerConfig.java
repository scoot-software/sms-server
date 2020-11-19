package com.scooter1556.sms.server.config;

import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private static final String API_VERSION = "1.2.0";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
          .select()
          .apis(RequestHandlerSelectors.basePackage("com.scooter1556.sms.server.controller"))
          .paths(PathSelectors.any())
          .build()
          .useDefaultResponseMessages(false)
          .apiInfo(apiInfo());
    }

    @Bean
    UiConfiguration uiConfig() {
      return UiConfigurationBuilder.builder()
          .defaultModelsExpandDepth(-1)
          .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
          "SMS REST API",
          "",
          API_VERSION,
          "",
          new Contact("Scoot Software", "https://github.com/scoot-software", "scoot.software@gmail.com"),
          "MIT", "https://raw.githubusercontent.com/scoot-software/sms-server/master/LICENSE", Collections.emptyList());
    }
}