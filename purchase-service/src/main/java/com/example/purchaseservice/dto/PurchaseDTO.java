package com.example.purchaseservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseDTO {
    private UserDTO buyer;
    private UserDTO seller;
    private BookDTO book;
}
