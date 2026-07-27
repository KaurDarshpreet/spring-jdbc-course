package com.stocks.model;

public record Stock(
    Long id,
    String symbol,
    String companyName,
    String sector,
    String exchange
) {}
