package com.example.purchaseservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseDTO {
    private String sellerEmail;
    private Long bookId;
}
