package com.northwind.discontinuedproducts.dto;

import lombok.Data;

@Data
public class DiscontinuedProduct {
    private Integer productId;
    private String productName;
    private double lastKnownPrice;
    private Integer lastKnownStock;
    private java.time.LocalDateTime archivedAt;
    private String archiveReason;
}
