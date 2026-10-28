package com.wellwithlife.omsbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EasyecomApiResponse {

    @JsonProperty("status")
    private String status; // success or error

    @JsonProperty("message")
    private String message;

    @JsonProperty("easyecom_order_id")
    private String easyecomOrderId;

    @JsonProperty("awb_number")
    private String awbNumber;

    @JsonProperty("shipping_partner")
    private String shippingPartner;

    @JsonProperty("tracking_url")
    private String trackingUrl;
}