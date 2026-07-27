package com.stocks.repository;

import com.stocks.model.Stock;

import java.util.List;
import java.util.Optional;


public interface StockRepository {

    List<Stock> findAll();

    Optional<Stock> findById(Long id);

    Optional<Stock> findBySymbol(String symbol);

    Stock save (Stock stock);

}
