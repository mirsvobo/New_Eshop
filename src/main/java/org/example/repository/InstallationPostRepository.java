package org.example.repository;

import org.example.model.InstallationPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstallationPostRepository extends JpaRepository<InstallationPost, Long> {
    List<InstallationPost> findAllByActiveTrueOrderByAssemblyDateDesc();
}