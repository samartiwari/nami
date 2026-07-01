package com.samar.nami.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 *
 * Scoped to the deployed frontend only (https://nami-frontend.pages.dev). Browsers
 * will let that site's JavaScript read responses from this API; other sites' browser
 * JS cannot. (This is a browser-enforced courtesy, not server security — direct
 * requests via curl/scripts are unaffected, which is fine for public read-only data.)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String FRONTEND_ORIGIN = "https://nami-frontend.pages.dev";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(FRONTEND_ORIGIN)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
