package com.example.fifoproject;

import java.time.LocalDate;

public class PaymentLog {
    private String paymentId;  // Her ödeme için benzersiz bir ID
    private int invoiceId;     // Kapatılan fatura ID'si
    private String customerId; // Müşteri ID'si
    private double paidAmount; // Yapılan ödeme miktarı
    private LocalDate paymentDate; // Ödeme tarihi

    public PaymentLog(String paymentId, int invoiceId, String customerId, double paidAmount) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.paidAmount = paidAmount;
        this.paymentDate = LocalDate.now(); // Ödeme tarihi otomatik olarak belirlenir
    }

    public String getPaymentId() {
        return paymentId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    // Log verilerini göster
    public void displayLog() {
        System.out.println("Ödeme ID: " + paymentId + ", Müşteri ID: " + customerId +
                ", Fatura ID: " + invoiceId + ", Ödenen Tutar: " + paidAmount +
                ", Ödeme Tarihi: " + paymentDate);
    }
}
