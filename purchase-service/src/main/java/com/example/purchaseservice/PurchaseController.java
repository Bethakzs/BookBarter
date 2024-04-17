package com.example.purchaseservice;

import com.example.purchaseservice.dto.PurchaseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/buy")
    @Transactional
    public Purchase buyBook(@RequestBody PurchaseDTO purchaseDTO,
                                     @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);
        String email = jwtTokenProvider.getEmailFromToken(token);
        Purchase purchase = Purchase.builder()
                .buyerEmail(email)
                .sellerEmail(purchaseDTO.getSellerEmail())
                .bookId(purchaseDTO.getBookId())
                .build();
        purchaseService.buyBook(purchase);
        return purchaseService.buyBook(purchase);
    }

    @PostMapping("/{email}/{id}")
    public ResponseEntity<?> confirmPurchase(@PathVariable String email , @PathVariable Long id) {
        purchaseService.confirmPurchase(id, email);
        return ResponseEntity.ok("Purchase confirmed");
    }
}
