package org.jirban.jira.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Kabir Khan
 */
public class CacheHeaderFilter implements Filter {

    private final String etagHex = Long.toHexString(System.currentTimeMillis());
    private static final int MAX_AGE = 60 * 5; //5 mins max age

    public CacheHeaderFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CaptureStatusHttpServletResponse captureStatusHttpServletResponse = new CaptureStatusHttpServletResponse((HttpServletResponse) response);

        HttpServletRequest req = (HttpServletRequest)request;
        String ifNoneMatch = req.getHeader("if-none-match");

        if (etagHex.equals(ifNoneMatch)) {
            ((HttpServletResponse) response).setStatus(304);
            return;
        }

        captureStatusHttpServletResponse.addHeader("ETag", etagHex);
        captureStatusHttpServletResponse.addHeader("Cache-Control", "max-age=30"); //TODO set to 5 minutes or so
        chain.doFilter(request, captureStatusHttpServletResponse);
        int status = captureStatusHttpServletResponse.status;
        if (status == 200) {
            captureStatusHttpServletResponse.addHeader("ETag", etagHex);
            captureStatusHttpServletResponse.addHeader("Cache-Control", "max-age=" + MAX_AGE); //TODO set to 5 minutes or so
        }
    }

    @Override
    public void destroy() {

    }

    private static class CaptureStatusHttpServletResponse implements HttpServletResponse {
        private final HttpServletResponse response;

        private int status;

        public CaptureStatusHttpServletResponse(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public void addCookie(Cookie cookie) {
            response.addCookie(cookie);
        }

        @Override
        public boolean containsHeader(String name) {
            return response.containsHeader(name);
        }

        @Override
        public String encodeURL(String url) {
            return response.encodeURL(url);
        }

        @Override
        public String encodeRedirectURL(String url) {
            return response.encodeRedirectURL(url);
        }

        @Override
        public String encodeUrl(String url) {
            return response.encodeUrl(url);
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return response.encodeRedirectUrl(url);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            response.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            response.sendError(sc);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            response.sendRedirect(location);
        }

        @Override
        public void setDateHeader(String name, long date) {
            response.setDateHeader(name, date);
        }

        @Override
        public void addDateHeader(String name, long date) {
            response.addDateHeader(name, date);
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            response.addHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            response.setIntHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            response.addIntHeader(name, value);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            response.setStatus(sc);
        }

        @Override
        public void setStatus(int sc, String sm) {
            this.status = sc;
            response.setStatus(sc, sm);
        }

        @Override
        public String getCharacterEncoding() {
            return response.getCharacterEncoding();
        }

        @Override
        public String getContentType() {
            return response.getContentType();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return response.getOutputStream();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return response.getWriter();
        }

        @Override
        public void setCharacterEncoding(String charset) {
            response.setCharacterEncoding(charset);
        }

        @Override
        public void setContentLength(int len) {
            response.setContentLength(len);
        }

        @Override
        public void setContentType(String type) {
            response.setContentType(type);
        }

        @Override
        public void setBufferSize(int size) {
            response.setBufferSize(size);
        }

        @Override
        public int getBufferSize() {
            return response.getBufferSize();
        }

        @Override
        public void flushBuffer() throws IOException {
            response.flushBuffer();
        }

        @Override
        public void resetBuffer() {
            response.resetBuffer();
        }

        @Override
        public boolean isCommitted() {
            return response.isCommitted();
        }

        @Override
        public void reset() {
            response.reset();
        }

        @Override
        public void setLocale(Locale loc) {
            response.setLocale(loc);
        }

        @Override
        public Locale getLocale() {
            return response.getLocale();
        }
    }
}


