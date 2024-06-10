package com.example.purchaseservice.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseDTO {
    private String sellerEmail;
    private Long bookId;
}
