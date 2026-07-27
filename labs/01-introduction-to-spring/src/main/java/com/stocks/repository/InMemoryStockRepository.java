package com.stocks.repository;

import com.stocks.model.Stock;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** * InMemoryStockRepository stores stocks in a Map. */
@Repository
public class InMemoryStockRepository implements StockRepository {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Stock> store = new ConcurrentHashMap<>();

    @Override
    public List<Stock> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Stock> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Stock> findBySymbol(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(stock -> stock.symbol() != null && stock.symbol().equalsIgnoreCase(symbol))
                .findFirst();
    }

    @Override
    public Stock save(Stock stock) {
        Stock toStore = stock;

        if (stock.id() == null) {
            long nextId = idSequence.getAndIncrement();
            // Adjust constructor args if your Stock record/class has more fields.
            toStore = new Stock(nextId, stock.symbol());
        }

        store.put(toStore.id(), toStore);
        return toStore;
    }
}

    
