package com.fintrack.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;
    @Column(name = "password", nullable = false)
    private String password;
    // Current balance needed for the logic check
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    private boolean isBlocked = false;

    private String fcmToken;

    @Column(name = "salt_edge_customer_id", unique = true)
    private String saltEdgeCustomerId;

    @Column(name = "salt_edge_connection_id")
    private String saltEdgeConnectionId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
}