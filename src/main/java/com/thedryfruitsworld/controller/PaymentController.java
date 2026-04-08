package com.thedryfruitsworld.controller;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.thedryfruitsworld.entity.Order;
import com.thedryfruitsworld.exception.BadRequestException;
import com.thedryfruitsworld.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    private final OrderService orderService;

    /**
     * Step 1: Create a Razorpay order.
     * Called before opening the Razorpay checkout modal.
     * Body: { amount: <paise> }
     * Returns: { razorpayOrderId, amount, currency, key }
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String userId) throws RazorpayException {

        int amount = Integer.parseInt(body.get("amount").toString());

        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject options = new JSONObject();
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));

        com.razorpay.Order rzpOrder = client.orders.create(options);
        String rzpOrderId = rzpOrder.get("id");

        return ResponseEntity.ok(Map.of(
                "razorpayOrderId", rzpOrderId,
                "amount", amount,
                "currency", "INR",
                "key", razorpayKeyId
        ));
    }

    /**
     * Step 2: Verify payment signature and place the DB order.
     * Called after Razorpay's handler callback fires on the frontend.
     * Body: { razorpayOrderId, razorpayPaymentId, razorpaySignature, items, paymentMethod, address, couponCode? }
     */
    @PostMapping("/verify")
    public ResponseEntity<Order> verifyAndPlaceOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String userId) throws Exception {

        String rzpOrderId   = (String) body.get("razorpayOrderId");
        String rzpPaymentId = (String) body.get("razorpayPaymentId");
        String rzpSignature = (String) body.get("razorpaySignature");

        if (rzpOrderId == null || rzpPaymentId == null || rzpSignature == null) {
            throw new BadRequestException("Missing Razorpay payment fields");
        }

        // Verify HMAC-SHA256 signature:  HMAC(orderId + "|" + paymentId, keySecret) == signature
        String payload = rzpOrderId + "|" + rzpPaymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String generatedSignature = bytesToHex(hash);

        if (!generatedSignature.equals(rzpSignature)) {
            throw new BadRequestException("Payment verification failed: invalid signature");
        }

        // Extract order payload from body
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        String couponCode = (String) body.get("couponCode");

        @SuppressWarnings("unchecked")
        Map<String, String> addressSnapshot = (Map<String, String>) body.get("address");

        Order order = orderService.createOrder(
                userId, items, "ONLINE", couponCode, addressSnapshot, rzpOrderId, rzpPaymentId);

        return ResponseEntity.ok(order);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
