package org.example.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "installation_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallationPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String productName;

    private LocalDate assemblyDate;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "installationPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<InstallationImage> images = new ArrayList<>();

    public void addImage(InstallationImage image) {
        images.add(image);
        image.setInstallationPost(this);
    }

    public void removeImage(InstallationImage image) {
        images.remove(image);
        image.setInstallationPost(null);
    }
}