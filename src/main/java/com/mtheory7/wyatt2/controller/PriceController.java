package com.mtheory7.wyatt2.controller;

import com.coinbase.advanced.model.products.GetProductRequest;
import com.coinbase.advanced.products.ProductsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/price")
public class PriceController {

    private final ProductsService productsService;

    public PriceController(ProductsService productsService) {
        this.productsService = productsService;
    }

    @GetMapping("/BTC")
    public String getBTCPrice() {
        return productsService.getProduct(new GetProductRequest.Builder().productId("BTC-USD").build()).getPrice();
    }
}
