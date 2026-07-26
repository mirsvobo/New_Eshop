package org.example.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "installation_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_post_id")
    private InstallationPost installationPost;
}