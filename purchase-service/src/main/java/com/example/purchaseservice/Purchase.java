package com.example.purchaseservice;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "purchases")
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "buyer_email", nullable = false)
    String buyerEmail;

    @Column(name = "seller_email", nullable = false)
    String sellerEmail;

    @Column(name = "book_id", nullable = false)
    Long bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    PurchaseStatus status;
}