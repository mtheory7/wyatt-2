package com.mtheory7.wyatt2.service;

import com.coinbase.advanced.model.orders.ListOrdersRequest;
import com.coinbase.advanced.model.orders.Order;
import com.coinbase.advanced.model.portfolios.ListPortfoliosRequest;
import com.coinbase.advanced.model.products.GetProductCandlesRequest;
import com.coinbase.advanced.model.products.GetProductCandlesResponse;
import com.coinbase.advanced.model.products.GetProductRequest;
import com.coinbase.advanced.orders.OrdersService;
import com.coinbase.advanced.portfolios.PortfoliosService;
import com.coinbase.advanced.products.ProductsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class Wyatt2Service {
    private static final Logger logger = LoggerFactory.getLogger(Wyatt2Service.class);
    private static final long SCHEDULED_TASK_TIMEOUT = 30000;
    private static final String BTC_USD_PRODUCT = "BTC-USD";
    private static final String GRANULARITY_ONE_MINUTE = "ONE_MINUTE";
    private static final String GRANULARITY_FIVE_MINUTE = "FIVE_MINUTE";

    private final OrdersService ordersService;
    private final PortfoliosService portfoliosService;
    private final ProductsService productsService;
    private final String portfolioUUID;

    public Wyatt2Service(OrdersService ordersService, PortfoliosService portfoliosService, ProductsService productsService) {
        this.ordersService = ordersService;
        this.productsService = productsService;
        this.portfoliosService = portfoliosService;
        this.portfolioUUID = portfoliosService.listPortfolios(new ListPortfoliosRequest()).getPortfolios().get(0).getUuid();
    }

    @Scheduled(fixedDelay = SCHEDULED_TASK_TIMEOUT)
    public void tradingLoop() {
        List<Order> openOrders = ordersService.listOrders(new ListOrdersRequest()).getOrders()
                .stream()
                .filter(order -> !order.isSettled())
                .toList();
        if (openOrders.isEmpty()) {
            GetProductCandlesRequest oneMinuteFiveHoursRequest = new GetProductCandlesRequest.Builder()
                    .productId(BTC_USD_PRODUCT)
                    .start(String.valueOf(Instant.now().minus(5, ChronoUnit.HOURS).getEpochSecond()))
                    .end(String.valueOf(Instant.now().getEpochSecond()))
                    .granularity(GRANULARITY_ONE_MINUTE)
                    .build();
            GetProductCandlesRequest fiveMinuteOneDayRequest = new GetProductCandlesRequest.Builder()
                    .productId(BTC_USD_PRODUCT)
                    .start(String.valueOf(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond()))
                    .end(String.valueOf(Instant.now().getEpochSecond()))
                    .granularity(GRANULARITY_FIVE_MINUTE)
                    .build();
            GetProductCandlesResponse oneMinuteFiveHoursCandleData = productsService.getProductCandles(oneMinuteFiveHoursRequest);
            GetProductCandlesResponse fiveMinuteOneDayCandleData = productsService.getProductCandles(fiveMinuteOneDayRequest);
            double currentPrice = Double.parseDouble(productsService.getProduct(new GetProductRequest.Builder().productId("BTC-USD").build()).getPrice());
            double oneMinuteFiveHoursMA = calculateMovingAverage(oneMinuteFiveHoursCandleData);
            double fiveMinuteOneDayMA = calculateMovingAverage(fiveMinuteOneDayCandleData);
            double maAverage = (oneMinuteFiveHoursMA + fiveMinuteOneDayMA) / 2;
            logger.trace("Moving average --- 5 hours --- 1 minute candles --- " + oneMinuteFiveHoursMA);
            logger.trace("Moving average --- 1 day   --- 5 minute candles --- " + fiveMinuteOneDayMA);
            logger.debug("Moving averages averaged: " + maAverage + " --- Current price: " + currentPrice);
            logger.debug("Would Wyatt2 sell? --- " + ((currentPrice > maAverage) ? "YES" : "NO"));
        } else {
            // Find out how long this order has been open
            // Either wait or cancel order and buy back at current price
        }
    }

    private double calculateMovingAverage(GetProductCandlesResponse candleStickData) {
        return (candleStickData.getCandles()
                .stream()
                .mapToDouble(candle -> Double.parseDouble(candle.getClose()))
                .sum()) / (candleStickData.getCandles().size());
    }
}
