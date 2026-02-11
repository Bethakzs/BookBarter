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
//	@Bean
//	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//		return builder.routes()
//				.route("user-service", r -> r.path("/api/user/**")
//						.uri("http://localhost:8082"))
//				.route("book-service", r -> r.path("/api/book/**")
//						.uri("http://localhost:8083"))
//				.route("wishlist-service", r -> r.path("/api/wishlist/**")
//						.uri("http://localhost:8084"))
//				.route("purchase-service", r -> r.path("/api/purchase/**")
//						.uri("http://localhost:8085"))
//				.route("review-service", r -> r.path("/api/review/**")
//						.uri("http://localhost:8086"))
//				.route("auth-service", r -> r.path("/auth/**")
//						.uri("http://localhost:8087"))
//				.route("notification-service", r -> r.path("/api/notifications/**")
//						.uri("http://localhost:8088"))
//				.build();
//	}

//	  For docker-compose
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service", r -> r.path("/api/user/**")
						.uri("lb://USER-SERVICE"))
				.route("book-service", r -> r.path("/api/book/**")
						.uri("lb://BOOK-SERVICE"))
				.route("wishlist-service", r -> r.path("/api/wishlist/**")
						.uri("lb://WISHLIST-SERVICE"))
				.route("purchase-service", r -> r.path("/api/purchase/**")
						.uri("lb://PURCHASE-SERVICE"))
				.route("review-service", r -> r.path("/api/review/**")
						.uri("lb://REVIEW-SERVICE"))
				.route("auth-service", r -> r.path("/auth/**")
						.uri("lb://AUTH-SERVICE"))
				.route("notification-service", r -> r.path("/api/notifications/**")
						.uri("lb://NOTIFICATION-SERVICE"))
				.build();
	}
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

}