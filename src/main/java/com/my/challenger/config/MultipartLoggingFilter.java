package com.my.challenger.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MultipartLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String contentType = httpRequest.getContentType();
            String uri = httpRequest.getRequestURI();

            if (uri.contains("/questions/with-media")) {
                log.info("🔍 [MultipartFilter] URI: {}", uri);
                log.info("🔍 [MultipartFilter] Content-Type: {}", contentType);
                log.info("🔍 [MultipartFilter] Content-Length: {}", httpRequest.getContentLength());

                if (contentType != null && contentType.contains("multipart")) {
                    try {
                        Collection<Part> parts = httpRequest.getParts();
                        log.info("🔍 [MultipartFilter] Number of parts: {}", parts.size());
                        for (Part part : parts) {
                            log.info("🔍 [MultipartFilter] Part: name={}, size={}, contentType={}, submittedFileName={}",
                                    part.getName(),
                                    part.getSize(),
                                    part.getContentType(),
                                    part.getSubmittedFileName());
                        }
                    } catch (Exception e) {
                        log.warn("🔍 [MultipartFilter] Could not get parts: {}", e.getMessage());
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
