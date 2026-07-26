package org.example.repository;

import org.example.model.InstallationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallationImageRepository extends JpaRepository<InstallationImage, Long> {
}