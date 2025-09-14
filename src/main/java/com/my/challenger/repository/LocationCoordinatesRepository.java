package com.my.challenger.repository;

import com.my.challenger.entity.challenge.LocationCoordinates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationCoordinatesRepository extends JpaRepository<LocationCoordinates, Long> {
}