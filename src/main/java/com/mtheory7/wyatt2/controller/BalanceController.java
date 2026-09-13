package com.mtheory7.wyatt2.controller;

import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownRequest;
import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownResponse;
import com.coinbase.advanced.model.portfolios.ListPortfoliosRequest;
import com.coinbase.advanced.model.portfolios.PortfolioBalances;
import com.coinbase.advanced.portfolios.PortfoliosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

    private final PortfoliosService portfoliosService;
    private final String portfolioUUID;

    public BalanceController(PortfoliosService portfoliosService) {
        this.portfoliosService = portfoliosService;
        this.portfolioUUID = portfoliosService.listPortfolios(new ListPortfoliosRequest()).getPortfolios().get(0).getUuid();
    }

    @GetMapping("/{asset}")
    public String getAssetBalance(@PathVariable String asset) {
        GetPortfolioBreakdownResponse balancesResponse = portfoliosService.getPortfolioBreakdown(new GetPortfolioBreakdownRequest(portfolioUUID));
        return BigDecimal.valueOf(balancesResponse.getBreakdown().getSpotPositions()
                .stream()
                .filter(spotPosition -> spotPosition.getAsset().equals(asset))
                .findFirst()
                .get()
                .getTotalBalanceCrypto()).toPlainString();
    }
}
