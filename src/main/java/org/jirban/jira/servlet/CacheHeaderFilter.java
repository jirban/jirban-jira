package org.jirban.jira.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Kabir Khan
 */
public class CacheHeaderFilter implements Filter {

    private final String etagHex = Long.toHexString(System.currentTimeMillis());
    private static final int MAX_AGE = 60 * 60 * 6; //6 hours max age

    public CacheHeaderFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest)request;
        String ifNoneMatch = req.getHeader("if-none-match");

        if (etagHex.equals(ifNoneMatch)) {
            ((HttpServletResponse) response).setStatus(304);
            return;
        }

        HttpServletResponse resp = (HttpServletResponse)response;
        resp.addHeader("ETag", etagHex);
        resp.addHeader("Cache-Control", "max-age=" + MAX_AGE);
        chain.doFilter(request, resp);
    }

    @Override
    public void destroy() {

    }
}


