// AuthenticationHandlerConfig.java (정리본)
package com.app.codemasterpiecebackend.security.config;

import com.app.codemasterpiecebackend.security.redirect.RuriSupport;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class AuthenticationHandlerConfig {

    @Value("${env.WEB_URL}")
    private String WEB_URL;

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        var base = new SavedRequestAwareAuthenticationSuccessHandler();
        base.setDefaultTargetUrl("/");
        base.setAlwaysUseDefaultTargetUrl(false);

        return (request, response, authentication) -> {
            String ruri = RuriSupport.safeRuri(request.getParameter("ruri"));
            if (ruri == null) ruri = RuriSupport.loadOnce(request); // 세션 백업에서 복구

            System.out.println("[LOGIN-SUCCESS] ruri=" + ruri);

            String path = (ruri != null) ? ruri : "/";

            var principal = (AppUserDetails) authentication.getPrincipal();
            String nickname = principal.getAppUser().nickname();
            if (nickname == null) nickname = "user";
            if (nickname.length() > 40) nickname = nickname.substring(0, 40);

            String qs = String.format(
                    "%slogin=success&code=%s&args=%s",
                    path.contains("?") ? "&" : "?",
                    URLEncoder.encode("auth.signin.success", StandardCharsets.UTF_8),
                    URLEncoder.encode(nickname, StandardCharsets.UTF_8)
            );
            response.sendRedirect(WEB_URL + (path.startsWith("/") ? path : "/" + path) + qs);
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, ex) -> {
            String ruri = RuriSupport.safeRuri(request.getParameter("ruri"));
            String path = (ruri != null) ? ruri : "/auth/signin";

            String code = switchCode(ex);
            String qs = String.format(
                    "%slogin=fail&code=%s",
                    path.contains("?") ? "&" : "?",
                    URLEncoder.encode(code, StandardCharsets.UTF_8)
            );
            response.sendRedirect(WEB_URL + (path.startsWith("/") ? path : "/" + path) + qs);
        };
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            String ruri = RuriSupport.safeRuri(request.getParameter("ruri"));
            String path = (ruri != null) ? ruri : "/";

            ResponseCookie cookie = ResponseCookie.from("JSESSIONID", "")
                    .maxAge(0).path("/").httpOnly(true).build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            String qs = String.format(
                    "%slogout=success&code=%s",
                    path.contains("?") ? "&" : "?",
                    URLEncoder.encode("auth.signout.success", StandardCharsets.UTF_8)
            );
            response.sendRedirect(WEB_URL + (path.startsWith("/") ? path : "/" + path) + qs);
        };
    }

    private static String switchCode(Exception ex) {
        if (ex instanceof BadCredentialsException) return "auth.error.bad_credentials";
        if (ex instanceof DisabledException) return "auth.error.disabled";
        if (ex instanceof LockedException) return "auth.error.locked";
        if (ex instanceof AccountExpiredException) return "auth.error.account_expired";
        if (ex instanceof CredentialsExpiredException) return "auth.error.credentials_expired";
        return "auth.error.failed";
    }
}
