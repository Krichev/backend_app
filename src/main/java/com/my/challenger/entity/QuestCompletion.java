//package com.my.challenger.entity;
//
//import jakarta.persistence.*;
//import java.sql.Timestamp;
//
//@Entity
//@Table(name = "quest_completion")
//public class QuestCompletion {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "quest_id", nullable = false)
//    private Long questId;
//
//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    @Column(nullable = false)
//    private String status; // e.g., accepted, completed
//
//    @Column(name = "completed_at")
//    private Timestamp completedAt;
//
//}