package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.StockMovementResponse;

import java.util.List;

public interface StockService {

    StockMovementResponse updateStock(StockUpdateRequest request);

    List<StockMovementResponse> getAllHistory();

    List<StockMovementResponse> getProductHistory(Integer productId);
}