package com.az.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/", "classpath:/images/")
                .setCachePeriod(0);
    }

//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//
//        registry.addMapping("/api/**")
//                .allowedOrigins("*")
//                .allowedMethods("GET", "POST","PUT","OPTION","DELETE")
////                .allowedHeaders("Authorization", "Origin","authorization")
//
//                .allowCredentials(false).maxAge(3600);
//
//        // Add more mappings...
//    }

}
