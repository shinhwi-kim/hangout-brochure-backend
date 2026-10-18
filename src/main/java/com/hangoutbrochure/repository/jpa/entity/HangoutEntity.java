package com.hangoutbrochure.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
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

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "source_raw_id")
    private UUID sourceRawId;

    @Column(name = "detail_url", columnDefinition = "text")
    private String detailUrl;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "location_name", nullable = false, length = 200)
    private String locationName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "event_time_desc", columnDefinition = "text")
    private String eventTimeDesc;

    @Column(name = "organizer", length = 200)
    private String organizer;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "is_free")
    private Boolean isFree;

    @Column(name = "fee_desc", columnDefinition = "text")
    private String feeDesc;

    @Column(name = "contact", length = 200)
    private String contact;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

    public String getSourceName() {
        return sourceName;
    }

    public UUID getSourceRawId() {
        return sourceRawId;
    }

    public String getDetailUrl() {
        return detailUrl;
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

    public String getDistrict() {
        return district;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getEventTimeDesc() {
        return eventTimeDesc;
    }

    public String getOrganizer() {
        return organizer;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public String getFeeDesc() {
        return feeDesc;
    }

    public String getContact() {
        return contact;
    }

    public String getStatus() {
        return status;
    }

    public Set<HangoutCategoryEntity> getCategories() {
        return categories;
    }
}
