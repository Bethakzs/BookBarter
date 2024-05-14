package com.example.apigatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayServiceApplication {
	//  For local VPN
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service", r -> r.path("/api/user/**")
						.uri("http://26.185.15.150:8082"))
				.route("book-service", r -> r.path("/api/book/**")
						.uri("http://26.185.15.150:8083"))
				.route("wishlist-service", r -> r.path("/api/wishlist/**")
						.uri("http://26.185.15.150:8084"))
				.route("purchase-service", r -> r.path("/api/purchase/**")
						.uri("http://26.185.15.150:8085"))
				.route("review-service", r -> r.path("/api/review/**")
						.uri("http://26.185.15.150:8086"))
				.route("auth-service", r -> r.path("/auth/**")
						.uri("http://26.185.15.150:8087"))
				.route("notification-service", r -> r.path("/api/notifications/**")
						.uri("http://26.185.15.150:8088"))
				.build();
	}

	//  For docker-compose
//	@Bean
//	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//		return builder.routes()
//				.route("user-service", r -> r.path("/api/user/**")
//						.uri("http://user-service:8082"))
//				.route("book-service", r -> r.path("/api/book/**")
//						.uri("http://book-service:8083"))
//				.route("wishlist-service", r -> r.path("/api/wishlist/**")
//						.uri("http://wishlist-service:8084"))
//				.route("purchase-service", r -> r.path("/api/purchase/**")
//						.uri("http://purchase-service:8085"))
//				.route("review-service", r -> r.path("/api/review/**")
//						.uri("http://review-service:8086"))
//				.route("auth-service", r -> r.path("/auth/**")
//						.uri("http://auth-service:8087"))
//				.build();
//	}
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

}