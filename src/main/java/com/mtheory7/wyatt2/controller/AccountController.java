package com.mtheory7.wyatt2.controller;

import com.coinbase.advanced.model.portfolios.GetPortfolioBreakdownRequest;
import com.coinbase.advanced.model.portfolios.ListPortfoliosRequest;
import com.coinbase.advanced.model.portfolios.ListPortfoliosResponse;
import com.coinbase.advanced.model.products.GetProductRequest;
import com.coinbase.advanced.model.products.GetProductResponse;
import com.coinbase.advanced.model.products.ListProductsRequest;
import com.coinbase.advanced.model.products.ListProductsResponse;
import com.coinbase.advanced.portfolios.PortfoliosService;
import com.coinbase.advanced.products.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private PortfoliosService portfoliosService;

    @Autowired
    private ProductsService productsService;

    @GetMapping("/balance")
    public String getBalance() throws Exception {
        ListPortfoliosRequest listReq = new ListPortfoliosRequest();
        ListPortfoliosResponse response = portfoliosService.listPortfolios(listReq);
        ListProductsResponse products = productsService.listProducts(new ListProductsRequest());
        GetProductResponse btcUsdResponse = productsService.getProduct(new GetProductRequest.Builder().productId("BTC-USD").build());
        return "Done";
    }
}
