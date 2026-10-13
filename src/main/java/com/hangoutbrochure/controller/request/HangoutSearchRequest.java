package com.hangoutbrochure.controller.request;

import com.hangoutbrochure.controller.request.validation.ValidHangoutSearch;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@ValidHangoutSearch
public class HangoutSearchRequest {
    private String keyword;
    private String category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime to;

    @DecimalMin("-180") @DecimalMax("180")
    private Double west;
    @DecimalMin("-90") @DecimalMax("90")
    private Double south;
    @DecimalMin("-180") @DecimalMax("180")
    private Double east;
    @DecimalMin("-90") @DecimalMax("90")
    private Double north;
    private String cursor;

    @Min(1) @Max(500)
    private int size = 300;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public OffsetDateTime getFrom() { return from; }
    public void setFrom(OffsetDateTime from) { this.from = from; }
    public OffsetDateTime getTo() { return to; }
    public void setTo(OffsetDateTime to) { this.to = to; }
    public Double getWest() { return west; }
    public void setWest(Double west) { this.west = west; }
    public Double getSouth() { return south; }
    public void setSouth(Double south) { this.south = south; }
    public Double getEast() { return east; }
    public void setEast(Double east) { this.east = east; }
    public Double getNorth() { return north; }
    public void setNorth(Double north) { this.north = north; }
    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
