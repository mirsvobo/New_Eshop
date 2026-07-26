package org.example.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @NotBlank(
            message = "Název příspěvku je povinný."
    )
    private String title;

    @NotBlank(
            message = "Název produktu je povinný."
    )
    private String productName;

    @NotNull(
            message = "Datum montáže je povinné."
    )
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
    )
    private LocalDate assemblyDate;

    @NotBlank(
            message = "Popis montáže je povinný."
    )
    @Column(
            columnDefinition = "TEXT"
    )
    private String content;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @OneToMany(
            mappedBy = "installationPost",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<InstallationImage> images =
            new ArrayList<>();

    public void addImage(
            InstallationImage image
    ) {
        if (image == null) {
            return;
        }

        if (images == null) {
            images = new ArrayList<>();
        }

        images.add(image);
        image.setInstallationPost(this);
    }

    public void removeImage(
            InstallationImage image
    ) {
        if (image == null || images == null) {
            return;
        }

        images.remove(image);
        image.setInstallationPost(null);
    }
}