package com.terminal_devilal.configurations.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GlobalCorsConfigTest {

	private final GlobalCorsConfig corsConfig = new GlobalCorsConfig();

	@Test
	void rejectsPreflightFromUnlistedLocalhostOrigin() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/devilal/ticker-info/details");
		request.addHeader("Origin", "http://localhost:3000");
		request.addHeader("Access-Control-Request-Method", "GET");
		MockHttpServletResponse response = new MockHttpServletResponse();

		corsConfig.corsFilter().doFilter(request, response, new MockFilterChain());

		assertEquals(403, response.getStatus());
	}

	@Test
	void allowsPostPreflightFromCurrentCloudflareTunnelHost() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/devilal/cache/pdv/reload");
		request.addHeader("Origin", "https://changing-tunnel.trycloudflare.com");
		request.addHeader("X-Forwarded-Host", "changing-tunnel.trycloudflare.com");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("Access-Control-Request-Method", "POST");
		request.addHeader("Access-Control-Request-Headers", "content-type");
		MockHttpServletResponse response = new MockHttpServletResponse();

		corsConfig.corsFilter().doFilter(request, response, new MockFilterChain());

		assertEquals(200, response.getStatus());
		assertEquals("https://changing-tunnel.trycloudflare.com",
				response.getHeader("Access-Control-Allow-Origin"));
		assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
	}

	@Test
	void rejectsCloudflareOriginThatDoesNotMatchRequestHost() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/devilal/cache/pdv/reload");
		request.addHeader("Origin", "https://another-tunnel.trycloudflare.com");
		request.addHeader("X-Forwarded-Host", "changing-tunnel.trycloudflare.com");
		request.addHeader("X-Forwarded-Proto", "https");
		request.addHeader("Access-Control-Request-Method", "POST");
		MockHttpServletResponse response = new MockHttpServletResponse();

		corsConfig.corsFilter().doFilter(request, response, new MockFilterChain());

		assertEquals(403, response.getStatus());
	}

	@Test
	void rejectsOriginsOutsideLoopback() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/devilal/ticker-info/details");
		request.addHeader("Origin", "https://example.com");
		request.addHeader("Access-Control-Request-Method", "GET");
		MockHttpServletResponse response = new MockHttpServletResponse();

		corsConfig.corsFilter().doFilter(request, response, new MockFilterChain());

		assertEquals(403, response.getStatus());
	}
}
