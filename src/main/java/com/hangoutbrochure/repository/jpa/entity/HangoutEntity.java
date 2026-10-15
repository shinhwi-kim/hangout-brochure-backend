package com.hangoutbrochure.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "hangouts")
public class HangoutEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hangout_category_assignments",
            joinColumns = @JoinColumn(name = "hangout_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<HangoutCategoryEntity> categories = new HashSet<>();

    protected HangoutEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public Set<HangoutCategoryEntity> getCategories() {
        return categories;
    }
}
