//package com.my.challenger.entity;
//
//import lombok.Getter;
//import lombok.Setter;
//import jakarta.persistence.*;
//import java.util.List;
//
//@Entity
//@Table(name = "wallets")
//@Getter
//@Setter
//public class Wallet {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private Double balance;
//
//    @OneToMany(mappedBy = "fromWallet")
//    private List<Transaction> outgoingTransactions;
//
//    @OneToMany(mappedBy = "toWallet")
//    private List<Transaction> incomingTransactions;
//}