package com.wellwithlife.omsbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellwithlife.omsbackend.entity.Order;
import com.wellwithlife.omsbackend.entity.OrderFulfillment;
import com.wellwithlife.omsbackend.entity.OrderItem;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.OrderFulfillmentRepository;
import com.wellwithlife.omsbackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class EasyecomService {

    private final WebClient easyecomWebClient;
    private final OrderRepository orderRepository;
    private final OrderFulfillmentRepository fulfillmentRepository;
    private final ObjectMapper objectMapper;

    @Value("${easyecom.api.x-api-key}")
    private String xApiKey;

    @Value("${easyecom.api.email}")
    private String apiEmail;

    @Value("${easyecom.api.password}")
    private String apiPassword;

    @Value("${easyecom.api.location-key}")
    private String locationKey;

    // In-memory token cache
    private String cachedJwtToken = null;

    /**
     * 1. JWT Token Generator & Cache Manager
     */
    public synchronized String getJwtToken() {
        if (this.cachedJwtToken != null) {
            return this.cachedJwtToken;
        }

        try {
            log.info("Generating new EasyEcom JWT token from /access/token...");

            Map<String, String> authBody = new HashMap<>();
            authBody.put("email", apiEmail);
            authBody.put("password", apiPassword);
            authBody.put("location_key", locationKey);

            String responseBody = easyecomWebClient.post()
                    .uri("/access/token")
                    .header("x-api-key", xApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(authBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode tokenNode = root.path("data").path("token").path("jwt_token");

            if (!tokenNode.isMissingNode() && !tokenNode.asText().isBlank()) {
                this.cachedJwtToken = tokenNode.asText();
                log.info("EasyEcom JWT Token generated and cached successfully!");
                return this.cachedJwtToken;
            } else {
                throw new IllegalStateException("JWT Token not present in response: " + responseBody);
            }
        } catch (WebClientResponseException ex) {
            log.error("EasyEcom Auth HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("EasyEcom Auth Failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to authenticate with EasyEcom: {}", ex.getMessage());
            throw new RuntimeException("EasyEcom Auth Failed: " + ex.getMessage());
        }
    }

    /**
     * 2. Push Order to EasyEcom (Admin/SuperAdmin trigger)
     */
    @Transactional
    public Order pushOrderToEasyecom(Long orderId, User adminUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if ("SYNCED_TO_EASYECOM".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already synced to Easyecom!");
        }

        // 1. Fetch JWT Token (Cached)
        String jwtToken = getJwtToken();

        // 2. Line Items Mapping with index-based safe unique item IDs
        AtomicInteger itemIndex = new AtomicInteger(1);
        List items = new ArrayList<>();

        for (OrderItem i : order.getItems()) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("OrderItemId", "ITEM-" + order.getReferenceCode() + "-" + itemIndex.getAndIncrement());
            itemMap.put("Sku", i.getSkuCode());
            itemMap.put("productName", (i.getProductName() != null && !i.getProductName().isBlank()) ? i.getProductName() : i.getSkuCode());
            itemMap.put("Quantity", i.getQuantity());
            itemMap.put("Price", Boolean.TRUE.equals(i.getIsFreeProduct()) ? 0 : i.getSellingPrice());
            itemMap.put("itemDiscount", 0);
            items.add(itemMap);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        String orderDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(formatter)
                : now.format(formatter);

        String expDeliveryDate = now.plusDays(3).format(formatter);

        // 3. Shipping Address Mapping (Priority: addressLine1 -> fallback to address)
        String shipAddr1 = (order.getAddressLine1() != null && !order.getAddressLine1().isBlank())
                ? order.getAddressLine1()
                : (order.getAddress() != null ? order.getAddress() : "Address");
        String shipAddr2 = order.getAddressLine2() != null ? order.getAddressLine2() : "";

        Map<String, Object> shipping = new LinkedHashMap<>();
        shipping.put("name", order.getCustomerName() != null ? order.getCustomerName() : "Customer");
        shipping.put("addressLine1", shipAddr1);
        shipping.put("addressLine2", shipAddr2);
        shipping.put("postalCode", order.getPincode() != null ? order.getPincode() : "");
        shipping.put("city", order.getCity() != null ? order.getCity() : "");
        shipping.put("state", order.getState() != null ? order.getState() : "");
        shipping.put("country", "India");
        shipping.put("contact", order.getPhone() != null ? order.getPhone() : "");
        shipping.put("email", (order.getEmail() != null && !order.getEmail().isBlank()) ? order.getEmail() : "support@wellwithlife.com");

        // 4. Billing Address Mapping (Fallback to shipping address if billing details are absent)
        Map<String, Object> billing = new LinkedHashMap<>();
        billing.put("name", (order.getBillingName() != null && !order.getBillingName().isBlank()) ? order.getBillingName() : shipping.get("name"));
        billing.put("addressLine1", (order.getBillingAddressLine1() != null && !order.getBillingAddressLine1().isBlank()) ? order.getBillingAddressLine1() : shipping.get("addressLine1"));
        billing.put("addressLine2", order.getBillingAddressLine2() != null ? order.getBillingAddressLine2() : shipping.get("addressLine2"));
        billing.put("postalCode", (order.getBillingPincode() != null && !order.getBillingPincode().isBlank()) ? order.getBillingPincode() : shipping.get("postalCode"));
        billing.put("city", (order.getBillingCity() != null && !order.getBillingCity().isBlank()) ? order.getBillingCity() : shipping.get("city"));
        billing.put("state", (order.getBillingState() != null && !order.getBillingState().isBlank()) ? order.getBillingState() : shipping.get("state"));
        billing.put("country", "India");
        billing.put("contact", (order.getBillingPhone() != null && !order.getBillingPhone().isBlank()) ? order.getBillingPhone() : shipping.get("contact"));
        billing.put("email", shipping.get("email"));

        Map<String, Object> customerObject = new LinkedHashMap<>();
        if (order.getGstNumber() != null && !order.getGstNumber().isBlank()) {
            customerObject.put("gst_number", order.getGstNumber());
        }
        customerObject.put("billing", billing);
        customerObject.put("shipping", shipping);

        List customerList = new ArrayList<>();
        customerList.add(customerObject);

        // 5. Payment Mode Sanitization
        String rawPaymentType = order.getPaymentType() != null ? order.getPaymentType().trim().toUpperCase() : "COD";
        boolean isCod = "COD".equals(rawPaymentType) || "CASH ON DELIVERY".equals(rawPaymentType) || "CASH".equals(rawPaymentType);
        int paymentModeId = isCod ? 2 : 1; // 2 = COD, 1 = Prepaid/Online in standard EasyEcom API

        // 6. EasyEcom Retail Order Payload Construction
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderType", "retailorder");
        payload.put("marketplaceId", 10);
        payload.put("orderNumber", order.getReferenceCode());
        payload.put("orderDate", orderDate);
        payload.put("expDeliveryDate", expDeliveryDate);
        payload.put("remarks1", order.getRemarks1() != null ? order.getRemarks1() : (order.getNotes() != null ? order.getNotes() : ""));
        payload.put("remarks2", order.getRemarks2() != null ? order.getRemarks2() : "");
        payload.put("shippingCost", order.getOtherCharges() != null ? order.getOtherCharges() : 0);
        payload.put("discount", order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
        payload.put("walletDiscount", 0);
        payload.put("promoCodeDiscount", 0);
        payload.put("prepaidDiscount", 0);
        payload.put("paymentMode", paymentModeId);

        // Gateway details are omitted completely for COD to satisfy EasyEcom validation
        if (!isCod) {
            payload.put("paymentGateway", "Online");
            payload.put("paymentTransactionNumber", "TXN-" + System.currentTimeMillis());
        }

        payload.put("shippingMethod", 1);
        payload.put("is_market_shipped", 0);
        payload.put("packageWeight", order.getPackageWeight() != null ? order.getPackageWeight() : 100);
        payload.put("packageHeight", order.getPackageHeight() != null ? order.getPackageHeight() : 10);
        payload.put("packageWidth", order.getPackageWidth() != null ? order.getPackageWidth() : 10);
        payload.put("packageLength", order.getPackageLength() != null ? order.getPackageLength() : 10);

        Object codCollectAmount = order.getAmountToCollect() != null ? order.getAmountToCollect() : order.getTotalAmount();
        payload.put("collectableAmount", isCod ? (codCollectAmount != null ? codCollectAmount : 0) : 0);
        payload.put("items", items);
        payload.put("customer", customerList);

        // 7. Fulfillment setup
        OrderFulfillment fulfillment = order.getFulfillment();
        if (fulfillment == null) {
            fulfillment = OrderFulfillment.builder()
                    .order(order)
                    .deliveryStatus("PENDING")
                    .build();
            order.setFulfillment(fulfillment);
        }

        try {
            String outgoingJson = objectMapper.writeValueAsString(payload);
            log.info("Sending payload to EasyEcom for order {}: {}", order.getReferenceCode(), outgoingJson);

            String rawResponse = easyecomWebClient.post()
                    .uri("/webhook/v2/createOrder")
                    .header("x-api-key", xApiKey)
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("EasyEcom response for {}: {}", order.getReferenceCode(), rawResponse);

            JsonNode root = objectMapper.readTree(rawResponse);
            int statusCode = root.path("code").asInt(root.path("Status").asInt(200));

            if (statusCode == 200) {
                order.setStatus("SYNCED_TO_EASYECOM");
                order.setApprovedBy(adminUser);

                String easyecomOrderId = root.path("data").path("OrderID").asText(
                        root.path("OrderID").asText(order.getReferenceCode())
                );
                fulfillment.setEasyecomOrderId(easyecomOrderId);
                fulfillment.setDeliveryStatus("PENDING");
                fulfillment.setSyncedAt(LocalDateTime.now());
                fulfillment.setSyncErrorPayload(null);
            } else {
                order.setStatus("SYNC_FAILED");
                fulfillment.setSyncErrorPayload(root.path("message").asText("Creation rejected by EasyEcom"));
            }
        } catch (WebClientResponseException ex) {
            log.error("EasyEcom push HTTP error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 401) {
                this.cachedJwtToken = null;
            }
            order.setStatus("SYNC_FAILED");
            fulfillment.setSyncErrorPayload(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to push order {} to EasyEcom: {}", order.getReferenceCode(), ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("401")) {
                this.cachedJwtToken = null;
            }
            order.setStatus("SYNC_FAILED");
            fulfillment.setSyncErrorPayload(ex.getMessage());
        }

        fulfillmentRepository.save(fulfillment);
        return orderRepository.save(order);
    }
    /**
     * 3. Fetch Real-time Products Master from EasyEcom
     */
    public JsonNode fetchProductMaster() {
        String jwtToken = getJwtToken();

        try {
            log.info("Fetching Product Master catalog from EasyEcom...");
            String response = easyecomWebClient.get()
                    .uri("/Products/GetProductMaster?custom_fields=1")
                    .header("x-api-key", xApiKey)
                    .header("Authorization", "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readTree(response);
        } catch (WebClientResponseException ex) {
            log.error("Product master HTTP error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 401) {
                this.cachedJwtToken = null;
            }
            throw new RuntimeException("EasyEcom Product Fetch Failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to fetch product master: {}", ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("401")) {
                this.cachedJwtToken = null;
            }
            throw new RuntimeException("EasyEcom Product Fetch Failed: " + ex.getMessage());
        }
    }

    /**
     * 4. EasyEcom Official V2 GetOrderDetails Endpoint
     * GET /orders/V2/getOrderDetails?reference_code={orderNumber}
     */
    public Map<String, Object> checkLiveOrderStatus(String orderNumber) {
        String jwtToken = getJwtToken();

        try {
            log.info("Checking live order status from EasyEcom V2 for order: {}", orderNumber);

            String responseStr = easyecomWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/orders/V2/getOrderDetails")
                            .queryParam("reference_code", orderNumber)
                            .build())
                    .header("x-api-key", xApiKey)
                    .header("Authorization", "Bearer " + jwtToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.debug("EasyEcom V2 Response for {}: {}", orderNumber, responseStr);

            JsonNode root = objectMapper.readTree(responseStr);
            int code = root.path("code").asInt(0);

            if (code != 200 || !root.has("data") || !root.path("data").isArray() || root.path("data").isEmpty()) {
                log.warn("Order {} not found or pending in EasyEcom: {}", orderNumber, root.path("message").asText());
                return Map.of("omsStatus", "IN_PROCESS");
            }

            JsonNode orderNode = root.path("data").get(0);

            String orderStatus = orderNode.path("order_status").asText("Open");
            int orderStatusId = orderNode.path("order_status_id").asInt(1);
            int fulfillableStatus = orderNode.path("fulfillable_status").asInt(1);

            String awbNumber = orderNode.path("awb_number").asText(
                    orderNode.path("tracking_number").asText(
                            orderNode.path("tracking_num").asText(null)
                    )
            );

            String courier = orderNode.path("courier").asText(
                    orderNode.path("carrier_name").asText(null)
            );

            String trackingUrl = orderNode.path("tracking_url").asText(null);
            if ((trackingUrl == null || trackingUrl.isBlank()) && awbNumber != null && !awbNumber.isBlank()) {
                if ("Delhivery".equalsIgnoreCase(courier)) {
                    trackingUrl = "https://www.delhivery.com/track/package/" + awbNumber;
                }
            }

            JsonNode documents = orderNode.path("documents");
            String labelUrl = documents.path("label").asText(null);
            String invoiceUrl = documents.path("easyecom_invoice").asText(null);

            Map<String, Object> result = new HashMap<>();
            result.put("rawStatus", orderStatus);
            result.put("orderStatusId", orderStatusId);
            result.put("fulfillableStatus", fulfillableStatus);
            result.put("awbNumber", awbNumber);
            result.put("courier", courier);
            result.put("trackingUrl", trackingUrl);
            result.put("labelUrl", labelUrl);
            result.put("invoiceUrl", invoiceUrl);

            if (fulfillableStatus == 0 || "Failed".equalsIgnoreCase(orderStatus)) {
                result.put("omsStatus", "STOCK_FAILED");
            } else if ("Ready to dispatch".equalsIgnoreCase(orderStatus) ||
                    "Dispatched".equalsIgnoreCase(orderStatus) ||
                    "Shipped".equalsIgnoreCase(orderStatus) ||
                    (awbNumber != null && !awbNumber.isBlank())) {
                result.put("omsStatus", "CONFIRMED_DISPATCHED");
            } else if ("Confirmed".equalsIgnoreCase(orderStatus) ||
                    "Open".equalsIgnoreCase(orderStatus) ||
                    "Assigned".equalsIgnoreCase(orderStatus) ||
                    "Printed".equalsIgnoreCase(orderStatus)) {
                result.put("omsStatus", "CONFIRMED");
            } else {
                result.put("omsStatus", "IN_PROCESS");
            }

            return result;

        } catch (WebClientResponseException ex) {
            log.error("EasyEcom V2 GetOrderDetails HTTP {} for {}: {}", ex.getStatusCode(), orderNumber, ex.getResponseBodyAsString());
            return Map.of("omsStatus", "CHECK_FAILED");
        } catch (Exception ex) {
            log.error("Failed to check EasyEcom live status for {}: {}", orderNumber, ex.getMessage());
            return Map.of("omsStatus", "CHECK_FAILED");
        }
    }
}