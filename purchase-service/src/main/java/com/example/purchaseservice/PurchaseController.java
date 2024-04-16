package com.example.purchaseservice;

import com.example.purchaseservice.dto.PurchaseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/")
    @Transactional
    public ResponseEntity<?> buyBook(@RequestBody Purchase purchase) {
        purchaseService.buyBook(purchase);
        return ResponseEntity.ok("Book bought successfully, wait when the seller confirms the purchase.");
    }

    @PostMapping("/{email}/{id}")
    public ResponseEntity<?> confirmPurchase(@PathVariable String email , @PathVariable Long id) {
        purchaseService.confirmPurchase(id, email);
        return ResponseEntity.ok("Purchase confirmed");
    }
}
