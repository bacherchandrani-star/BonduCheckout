
package com.bondhubazar.checkout.bondhucheckout;

import com.google.appinventor.components.annotations.Extension;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import org.json.JSONObject;

@Extension(
    description = "Bondhu Bazar Checkout Extension"
)
public class BondhuCheckout extends AndroidNonvisibleComponent {

    // ডেলিভারি ঠিকানার তথ্য
    private String addressId = "";
    private String addressText = "";
    private String phone = "";

    // Cart-এর তথ্য
    private int itemCount = 0;
    private long subtotalPaise = 0;

    // অতিরিক্ত হিসাব
    private long deliveryFeePaise = 0;
    private long discountPaise = 0;

    // Checkout-এর অবস্থা
    private boolean orderConfirmed = false;

    public BondhuCheckout(ComponentContainer container) {
        super(container.$form());
    }

    // =====================================
    // ১. ডেলিভারি ঠিকানা সেট করা
    // =====================================

    @SimpleFunction(
        description = "ডেলিভারি ঠিকানা ও ফোন নম্বর সেট করবে"
    )
    public void SetDeliveryAddress(
        String id,
        String address,
        String mobile
    ) {
        addressId = safe(id);
        addressText = safe(address);
        phone = safe(mobile);

        orderConfirmed = false;
    }

    // =====================================
    // ২. Cart-এর হিসাব সেট করা
    // =====================================

    @SimpleFunction(
        description = "Cart-এর পণ্যের সংখ্যা এবং Subtotal সেট করবে"
    )
    public void SetCartSummary(
        int count,
        long subtotal
    ) {
        itemCount = Math.max(0, count);
        subtotalPaise = Math.max(0, subtotal);

        orderConfirmed = false;
    }

    // =====================================
    // ৩. Delivery Charge সেট করা
    // =====================================

    @SimpleFunction(
        description = "Delivery charge পয়সায় সেট করবে"
    )
    public void SetDeliveryFee(long fee) {
        deliveryFeePaise = Math.max(0, fee);
        orderConfirmed = false;
    }

    // =====================================
    // ৪. Discount সেট করা
    // =====================================

    @SimpleFunction(
        description = "Discount পয়সায় সেট করবে"
    )
    public void SetDiscount(long discount) {
        discountPaise = Math.max(0, discount);
        orderConfirmed = false;
    }

    // =====================================
    // ৫. Grand Total হিসাব
    // =====================================

    @SimpleFunction(
        description = "সর্বমোট দাম পয়সায় ফেরত দেবে"
    )
    public long GetGrandTotalPaise() {

        long total =
            subtotalPaise +
            deliveryFeePaise -
            discountPaise;

        return Math.max(0, total);
    }

    // =====================================
    // ৬. ফোন নম্বর যাচাই
    // =====================================

    private boolean isValidPhone() {

        // শুধু সংখ্যাগুলো রাখা হচ্ছে
        String number = phone.replaceAll("[^0-9]", "");

        // ভারতীয় ১০ সংখ্যার মোবাইল নম্বর
        return number.matches("[6-9][0-9]{9}");
    }

    // =====================================
    // ৭. Checkout Validation
    // =====================================

    @SimpleFunction(
        description = "Checkout-এর সব তথ্য যাচাই করবে"
    )
    public boolean ValidateCheckout() {

        if (addressId.trim().isEmpty()) {
            CheckoutFailed("ডেলিভারি ঠিকানা নির্বাচন করো");
            return false;
        }

        if (addressText.trim().isEmpty()) {
            CheckoutFailed("ডেলিভারি ঠিকানা পাওয়া যায়নি");
            return false;
        }

        if (!isValidPhone()) {
            CheckoutFailed("সঠিক ১০ সংখ্যার ফোন নম্বর দাও");
            return false;
        }

        if (itemCount <= 0) {
            CheckoutFailed("তোমার Cart খালি");
            return false;
        }

        if (subtotalPaise <= 0) {
            CheckoutFailed("অর্ডারের দাম সঠিক নয়");
            return false;
        }

        if (discountPaise > subtotalPaise + deliveryFeePaise) {
            CheckoutFailed("Discount-এর পরিমাণ সঠিক নয়");
            return false;
        }

        if (GetGrandTotalPaise() <= 0) {
            CheckoutFailed("অর্ডারের মোট দাম সঠিক নয়");
            return false;
        }

        return true;
    }

    // =====================================
    // ৮. Order Confirm
    // =====================================

    @SimpleFunction(
        description = "সব তথ্য সঠিক হলে Checkout Confirm করবে"
    )
    public boolean ConfirmCheckout() {

        orderConfirmed = false;

        if (!ValidateCheckout()) {
            return false;
        }

        try {

            JSONObject data = new JSONObject();

            data.put("addressId", addressId);
            data.put("address", addressText);
            data.put("phone", phone);

            data.put("itemCount", itemCount);

            data.put("subtotalPaise", subtotalPaise);
            data.put("deliveryFeePaise", deliveryFeePaise);
            data.put("discountPaise", discountPaise);
            data.put("grandTotalPaise", GetGrandTotalPaise());

            data.put("currency", "INR");

            orderConfirmed = true;

            // BondhuOrder Extension-কে তথ্য পাঠানোর জন্য Event
            CheckoutConfirmed(data.toString());

            return true;

        } catch (Exception e) {

            CheckoutFailed("Checkout তৈরি করতে সমস্যা হয়েছে");
            return false;
        }
    }

    // =====================================
    // ৯. Confirm হয়েছে কি না
    // =====================================

    @SimpleFunction(
        description = "Checkout Confirm হয়েছে কি না জানাবে"
    )
    public boolean IsOrderConfirmed() {
        return orderConfirmed;
    }

    // =====================================
    // ১০. Checkout বাতিল
    // =====================================

    @SimpleFunction(
        description = "Checkout-এর বর্তমান তথ্য Reset করবে"
    )
    public void ResetCheckout() {

        addressId = "";
        addressText = "";
        phone = "";

        itemCount = 0;
        subtotalPaise = 0;

        deliveryFeePaise = 0;
        discountPaise = 0;

        orderConfirmed = false;

        CheckoutReset();
    }

    // =====================================
    // ১১. খালি String নিরাপদে সামলানো
    // =====================================

    private String safe(String value) {

        return value == null ? "" : value.trim();
    }

    // =====================================
    // EVENTS
    // =====================================

    @SimpleEvent(
        description = "Checkout সফল হলে এই Event চলবে"
    )
    public void CheckoutConfirmed(String checkoutJson) {

        EventDispatcher.dispatchEvent(
            this,
            "CheckoutConfirmed",
            checkoutJson
        );
    }

    @SimpleEvent(
        description = "Checkout-এ কোনো ভুল হলে এই Event চলবে"
    )
    public void CheckoutFailed(String reason) {

        EventDispatcher.dispatchEvent(
            this,
            "CheckoutFailed",
            reason
        );
    }

    @SimpleEvent(
        description = "Checkout Reset হলে এই Event চলবে"
    )
    public void CheckoutReset() {

        EventDispatcher.dispatchEvent(
            this,
            "CheckoutReset"
        );
    }
}
