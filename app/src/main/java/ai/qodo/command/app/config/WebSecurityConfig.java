/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.io.IOException;
import java.util.Set;

/**
 * Web security configuration to prevent multipart upload exploits.
 * Only allows multipart requests on explicitly allowlisted paths.
 */
@Configuration
public class WebSecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
    
    /**
     * Allowlist of paths that are permitted to accept multipart uploads.
     * Add any endpoints that legitimately need file upload capability here.
     */
    private static final Set<String> MULTIPART_ALLOWED_PATHS = Set.of(
    );
    
    /**
     * Configure the MultipartResolver bean.
     * This uses the standard servlet multipart resolver.
     */
    @Bean
    public MultipartResolver multipartResolver() {
        logger.info("Configuring StandardServletMultipartResolver with path-based filtering");
        return new StandardServletMultipartResolver();
    }
    
    /**
     * Register a filter to block multipart requests unless the path is in the allowlist.
     * This prevents multipart upload exploits on endpoints that don't need file uploads.
     */
    @Bean
    public FilterRegistrationBean<MultipartUploadFilter> multipartUploadFilter() {
        FilterRegistrationBean<MultipartUploadFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MultipartUploadFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.setName("multipartUploadFilter");
        
        logger.info("Registered MultipartUploadFilter with allowlist: {}", MULTIPART_ALLOWED_PATHS);
        
        return registrationBean;
    }
    
    /**
     * Filter that blocks multipart requests on non-allowlisted paths.
     */
    public static class MultipartUploadFilter implements Filter {
        
        private static final Logger logger = LoggerFactory.getLogger(MultipartUploadFilter.class);
        private static final String MULTIPART_CONTENT_TYPE = "multipart/";
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String contentType = httpRequest.getContentType();
            String requestPath = httpRequest.getRequestURI();
            
            // Check if this is a multipart request
            if (contentType != null && contentType.toLowerCase().startsWith(MULTIPART_CONTENT_TYPE)) {
                
                // Check if the path is in the allowlist
                boolean isAllowed = isPathAllowed(requestPath);
                
                if (!isAllowed) {
                    logger.warn("Blocked multipart upload attempt on non-allowlisted path: {} from IP: {}", 
                               requestPath, httpRequest.getRemoteAddr());
                    
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(
                        "{\"error\":\"Multipart uploads are not allowed on this endpoint\",\"status\":403}"
                    );
                    return;
                }
                
                logger.debug("Allowing multipart upload on allowlisted path: {}", requestPath);
            }
            
            chain.doFilter(request, response);
        }
        
        /**
         * Check if the request path is in the allowlist.
         * Supports exact matches and prefix matches (paths ending with /*).
         */
        private boolean isPathAllowed(String requestPath) {
            for (String allowedPath : MULTIPART_ALLOWED_PATHS) {
                // Exact match
                if (allowedPath.equals(requestPath)) {
                    return true;
                }
                
                // Prefix match (e.g., /api/upload/* matches /api/upload/file)
                if (allowedPath.endsWith("/*")) {
                    String prefix = allowedPath.substring(0, allowedPath.length() - 2);
                    if (requestPath.startsWith(prefix)) {
                        return true;
                    }
                }

            }
            
            return false;
        }
    }
}
