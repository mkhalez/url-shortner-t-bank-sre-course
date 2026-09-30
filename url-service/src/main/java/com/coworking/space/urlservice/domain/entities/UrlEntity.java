package com.coworking.space.urlservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "url")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
public class UrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String shortUrl;

    private String longUrl;

    @CreatedDate
    @Column(name ="created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name ="last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;
}
