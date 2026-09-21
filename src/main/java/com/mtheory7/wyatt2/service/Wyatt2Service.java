package com.mtheory7.wyatt2.service;

import com.coinbase.advanced.model.orders.*;
import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownRequest;
import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownResponse;
import com.coinbase.advanced.model.portfolios.ListPortfoliosRequest;
import com.coinbase.advanced.model.portfolios.SpotPosition;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final DecimalFormat df8;
    private final DecimalFormat df2;

    public Wyatt2Service(OrdersService ordersService, PortfoliosService portfoliosService, ProductsService productsService) {
        this.ordersService = ordersService;
        this.productsService = productsService;
        this.portfoliosService = portfoliosService;
        this.portfolioUUID = portfoliosService.listPortfolios(new ListPortfoliosRequest()).getPortfolios().get(0).getUuid();
        df8 = new DecimalFormat("#.########");
        df8.setRoundingMode(RoundingMode.DOWN);
        df2 = new DecimalFormat("#.#");
        df2.setRoundingMode(RoundingMode.DOWN);
    }

    @Scheduled(fixedDelay = SCHEDULED_TASK_TIMEOUT)
    public void tradingLoop() throws InterruptedException {
        List<Order> openOrders = ordersService.listOrders(new ListOrdersRequest()).getOrders()
                .stream()
                .filter(order -> !order.isSettled() && !order.getStatus().equals("CANCELLED"))
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
            if (currentPrice > maAverage) {
                double sellPrice = (Math.round(currentPrice * 100.0) / 100.0);
                double buyBackPrice = (Math.round((currentPrice * 0.975) * 100.0) / 100.0);
                // Execute sell at current price
                CreateOrderRequest sellRequest = new CreateOrderRequest.Builder()
                        .productId(BTC_USD_PRODUCT)
                        .clientOrderId(UUID.randomUUID().toString())
                        .retailPortfolioId(portfolioUUID)
                        .side("SELL")
                        .orderConfiguration(new OrderConfiguration.Builder()
                                .limitLimitGtc(new LimitGtc.Builder()
                                        .limitPrice(String.valueOf(sellPrice))
                                        .baseSize(getBTCBalance())
                                        .build())
                                .build())
                        .build();
                CreateOrderResponse sellResponse = ordersService.createOrder(sellRequest);
                logger.debug("Sell order placed! Waiting 5 seconds...");
                TimeUnit.SECONDS.sleep(5);
                // Execute sell at current price
                CreateOrderRequest buyRequest = new CreateOrderRequest.Builder()
                        .productId(BTC_USD_PRODUCT)
                        .clientOrderId(UUID.randomUUID().toString())
                        .retailPortfolioId(portfolioUUID)
                        .side("BUY")
                        .orderConfiguration(new OrderConfiguration.Builder()
                                .limitLimitGtc(new LimitGtc.Builder()
                                        .limitPrice(String.valueOf(buyBackPrice))
                                        .quoteSize(getUSDBalance())
                                        .build())
                                .build())
                        .build();
                CreateOrderResponse buyBackResponse = ordersService.createOrder(buyRequest);
                logger.debug("Buy-back order placed!");
            }
        } else {
            logger.debug("Open order exists waiting to be filled. Waiting 30 seconds...");
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

    private String getBTCBalance() {
        GetPortfolioBreakdownResponse balancesResponse = portfoliosService.getPortfolioBreakdown(new GetPortfolioBreakdownRequest(portfolioUUID));
        Optional<SpotPosition> spotPositionOptional = balancesResponse.getBreakdown().getSpotPositions()
                .stream()
                .filter(spotPosition -> spotPosition.getAsset().equals("BTC"))
                .findFirst();
        return spotPositionOptional.map(spotPosition -> df8.format(BigDecimal.valueOf(spotPosition.getTotalBalanceCrypto()))).orElse("0.0");
    }

    private String getUSDBalance() {
        GetPortfolioBreakdownResponse balancesResponse = portfoliosService.getPortfolioBreakdown(new GetPortfolioBreakdownRequest(portfolioUUID));
        Optional<SpotPosition> spotPositionOptional = balancesResponse.getBreakdown().getSpotPositions()
                .stream()
                .filter(spotPosition -> spotPosition.getAsset().equals("USD") && spotPosition.getAccountType().equals("ACCOUNT_TYPE_FIAT"))
                .findFirst();
        return spotPositionOptional.map(spotPosition -> df2.format(BigDecimal.valueOf(spotPosition.getTotalBalanceCrypto()))).orElse("0.0");
    }
}
