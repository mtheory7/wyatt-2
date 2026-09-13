package com.mtheory7.wyatt2.controller;

import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownRequest;
import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownResponse;
import com.coinbase.advanced.model.portfolios.ListPortfoliosRequest;
import com.coinbase.advanced.model.products.GetProductRequest;
import com.coinbase.advanced.model.products.GetProductResponse;
import com.coinbase.advanced.portfolios.PortfoliosService;
import com.coinbase.advanced.products.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private PortfoliosService portfoliosService;

    @Autowired
    private ProductsService productsService;

    private String portfolioUUID = null;

    @GetMapping("/balance")
    public String getBalance() throws Exception {
        if (portfolioUUID == null) {
            portfolioUUID = portfoliosService.listPortfolios(
                    new ListPortfoliosRequest()).getPortfolios().get(0).getUuid();
        }
        GetPortfolioBreakdownResponse balancesResponse = portfoliosService.getPortfolioBreakdown(new GetPortfolioBreakdownRequest(portfolioUUID));
        GetProductResponse btcUsdResponse = productsService.getProduct(new GetProductRequest.Builder().productId("BTC-USD").build());
        String response = "BTC Balance: " + BigDecimal.valueOf(balancesResponse.getBreakdown().getSpotPositions()
                .stream()
                .filter(spotPosition -> spotPosition.getAsset().equals("BTC"))
                .findFirst()
                .get()
                .getTotalBalanceCrypto()).toPlainString();
        response += "<br>USD Balance: " + balancesResponse.getBreakdown().getSpotPositions()
                .stream()
                .filter(spotPosition -> spotPosition.getAsset().equals("USD"))
                .findFirst()
                .get()
                .getTotalBalanceCrypto();
        response += "<br>BTC Price: $" + btcUsdResponse.getPrice();
        return response;
    }
}
