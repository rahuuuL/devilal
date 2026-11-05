package com.terminal_devilal.configurations.proxy;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

	@Bean
	public CorsFilter corsFilter() {
		CorsConfigurationSource source = request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowCredentials(true);
			config.setAllowedOriginPatterns(List.of("http://[::1]:[*]"));
			config.setAllowedHeaders(List.of("*"));
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

			String origin = request.getHeader("Origin");
			if (isSameOrigin(origin, request)) {
				config.addAllowedOrigin(origin);
			}
			return config;
		};
		return new CorsFilter(source);
	}

	private boolean isSameOrigin(String origin, HttpServletRequest request) {
		if (origin == null) {
			return false;
		}

		try {
			URI originUri = URI.create(origin);
			if (originUri.getHost() == null || originUri.getRawUserInfo() != null
					|| originUri.getRawPath() != null && !originUri.getRawPath().isEmpty()
					|| originUri.getRawQuery() != null || originUri.getRawFragment() != null) {
				return false;
			}

			String forwardedHost = firstForwardedValue(request.getHeader("X-Forwarded-Host"));
			String requestHost = forwardedHost != null ? forwardedHost : request.getHeader("Host");
			if (requestHost == null || !originUri.getRawAuthority().equalsIgnoreCase(requestHost)) {
				return false;
			}

			String scheme = originUri.getScheme();
			String forwardedProto = firstForwardedValue(request.getHeader("X-Forwarded-Proto"));
			String requestScheme = forwardedProto != null ? forwardedProto : request.getScheme();
			return scheme != null && (scheme.equalsIgnoreCase(requestScheme)
					|| scheme.equalsIgnoreCase("https")
							&& originUri.getHost().toLowerCase(Locale.ROOT).endsWith(".trycloudflare.com"));
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private String firstForwardedValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.split(",", 2)[0].trim();
	}
}