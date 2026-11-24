package com.exam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SqlCountFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        AtomicInteger counter = new AtomicInteger();
        SqlCountInterceptor.COUNT.set(counter);
        try {
            chain.doFilter(request, response);
            response.setHeader("X-Sql-Count", String.valueOf(counter.get()));
        } finally {
            SqlCountInterceptor.COUNT.remove();
        }
    }
}
