package com.example.fifoproject;

import java.time.LocalDate;

public class PaymentLog {
    private int invoiceId;
    private double paidAmount;
    private LocalDate paymentDate;

    public PaymentLog(int invoiceId, double paidAmount) {
        this.invoiceId = invoiceId;
        this.paidAmount = paidAmount;
        this.paymentDate = LocalDate.now(); // Ödeme tarihi otomatik olarak belirlenir
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    // Log verilerini göster
    public void displayLog() {
        System.out.println("Fatura ID: " + invoiceId + ", Ödenen Tutar: " + paidAmount + ", Ödeme Tarihi: " + paymentDate);
    }
}
