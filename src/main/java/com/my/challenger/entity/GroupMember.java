//package com.my.challenger.entity;
//
//import lombok.Getter;
//import lombok.Setter;
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "group_members")
//@Getter
//@Setter
//public class GroupMember {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "group_id")
//    private Group group;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
//
//    @Enumerated(EnumType.STRING)
//    private GroupRole role;
//
//    private LocalDateTime joinedAt;
//}
//
//enum GroupRole {
//    ADMIN, MEMBER
//}