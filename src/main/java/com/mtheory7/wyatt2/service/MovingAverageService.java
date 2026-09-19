package com.mtheory7.wyatt2.service;

import com.coinbase.advanced.model.products.Candle;
import com.coinbase.advanced.model.products.GetProductCandlesRequest;
import com.coinbase.advanced.model.products.GetProductCandlesResponse;
import com.coinbase.advanced.model.products.GetProductRequest;
import com.coinbase.advanced.products.ProductsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class MovingAverageService {
    private static final Logger logger = LoggerFactory.getLogger(MovingAverageService.class);
    private static final long SCHEDULED_TASK_TIMEOUT = 30000;
    private static final String BTC_USD_PRODUCT = "BTC-USD";
    private static final String GRANULARITY_ONE_MINUTE = "ONE_MINUTE";
    private static final String GRANULARITY_FIVE_MINUTE = "FIVE_MINUTE";

    private final ProductsService productsService;

    public MovingAverageService(ProductsService productsService) {
        this.productsService = productsService;
    }

    @Scheduled(fixedDelay = SCHEDULED_TASK_TIMEOUT)
    public void executeWithDelay() {
        GetProductCandlesRequest oneMinuteOneDayRequest = new GetProductCandlesRequest.Builder()
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
        GetProductCandlesResponse oneMinuteOneDayCandleData = productsService.getProductCandles(oneMinuteOneDayRequest);
        GetProductCandlesResponse fiveMinuteOneDayCandleData = productsService.getProductCandles(fiveMinuteOneDayRequest);
        double currentPrice = Double.parseDouble(productsService.getProduct(new GetProductRequest.Builder().productId("BTC-USD").build()).getPrice());
        double oneMinuteOneDayMA = calculateMovingAverage(oneMinuteOneDayCandleData);
        double fiveMinuteOneDayMA = calculateMovingAverage(fiveMinuteOneDayCandleData);
        double maAverage = (oneMinuteOneDayMA + fiveMinuteOneDayMA) / 2;
        logger.trace("Moving average --- 5 hours --- 1 minute candles --- " + oneMinuteOneDayMA);
        logger.trace("Moving average --- 1 day   --- 5 minute candles --- " + fiveMinuteOneDayMA);
        logger.debug("Moving averages averaged: " + maAverage + " --- Current price: " + currentPrice);
        logger.debug("Would Wyatt2 sell? --- " + ((maAverage < currentPrice) ? "YES" : "NO"));
    }

    private double calculateMovingAverage(GetProductCandlesResponse candleStickData) {
        return (candleStickData.getCandles().stream()
                .mapToDouble(candle -> Double.parseDouble(candle.getClose()))
                .sum()) / (candleStickData.getCandles().size());
    }
}
