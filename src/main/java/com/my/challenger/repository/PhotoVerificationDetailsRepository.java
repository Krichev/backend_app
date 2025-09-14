package com.my.challenger.repository;

import com.my.challenger.entity.challenge.PhotoVerificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoVerificationDetailsRepository extends JpaRepository<PhotoVerificationDetails, Long> {
}