package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to log service discovery routing.
 * 
 * Shows when requests are routed via Consul service discovery (lb:// scheme).
 */
@Component
@Slf4j
public class ServiceDiscoveryLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Log after route is resolved (in the response phase)
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            
            if (route != null) {
                var uri = route.getUri();
                var scheme = uri.getScheme();
                var host = uri.getHost();
                
                // Log when using service discovery (lb:// scheme)
                if ("lb".equals(scheme)) {
                    log.info("🔍 Service Discovery: Routing {} to service '{}' via Consul (lb://{})", 
                        exchange.getRequest().getPath(), 
                        host, 
                        host);
                }
            } else {
                // Log all requests to see what's happening
                log.debug("Request to {} - no route matched", exchange.getRequest().getPath());
            }
        }));
    }

    @Override
    public int getOrder() {
        // Execute after routing is complete
        return Ordered.LOWEST_PRECEDENCE;
    }
}

