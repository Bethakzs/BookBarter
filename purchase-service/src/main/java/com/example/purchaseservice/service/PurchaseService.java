package com.example.purchaseservice.service;

import com.example.purchaseservice.entity.Purchase;
import org.springframework.stereotype.Service;

@Service
public interface PurchaseService {
    Purchase buyBook(Purchase purchase);

    void confirmPurchase(Long id, String email);
}
