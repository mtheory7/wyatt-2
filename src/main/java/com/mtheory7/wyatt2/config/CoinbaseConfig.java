package com.mtheory7.wyatt2.config;

import com.coinbase.advanced.client.CoinbaseAdvancedClient;
import com.coinbase.advanced.credentials.CoinbaseAdvancedCredentials;
import com.coinbase.advanced.factory.CoinbaseAdvancedServiceFactory;
import com.coinbase.advanced.orders.OrdersService;
import com.coinbase.advanced.portfolios.PortfoliosService;
import com.coinbase.advanced.products.ProductsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinbaseConfig {

    @Bean
    public CoinbaseAdvancedClient coinbaseAdvancedClient() {
        return new CoinbaseAdvancedClient(new CoinbaseAdvancedCredentials(System.getenv("ADVANCED_TRADE_CREDENTIALS")));
    }

    @Bean
    public PortfoliosService portfolioService(CoinbaseAdvancedClient client) {
        return CoinbaseAdvancedServiceFactory.createPortfoliosService(client);
    }

    @Bean
    public ProductsService productsService(CoinbaseAdvancedClient client) {
        return CoinbaseAdvancedServiceFactory.createProductsService(client);
    }

    @Bean
    public OrdersService ordersService(CoinbaseAdvancedClient client) {
        return CoinbaseAdvancedServiceFactory.createOrdersService(client);
    }
}
