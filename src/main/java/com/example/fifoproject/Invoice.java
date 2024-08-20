package com.example.fifoproject;

import java.time.LocalDate;

public class Invoice {
    private double amount; // Ana borç
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private boolean isPaid;
    private static final double INTEREST_RATE = 0.02; // %2 gecikme faizi

    public Invoice(double amount, LocalDate dueDate) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = false;
    }

    public double getAmount() {
        return amount; // Ana para sabit kalacak
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isPaid() {
        return isPaid;
    }

    // Gecikme faizi hesaplama
    public double calculateLateFee() {
        if (isOverdue()) {
            long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
            return overdueDays * INTEREST_RATE * amount; // Gecikme günleri üzerinden faiz
        }
        return 0;
    }

    // Ana para ve gecikme faizini ayrı gösteren toplam borç
    public double getTotalDebtWithLateFee() {
        return amount + calculateLateFee(); // Ana borca gecikme faizi eklenir
    }

    public boolean isOverdue() {
        return !isPaid && LocalDate.now().isAfter(dueDate);
    }

    public void makePayment(double paymentAmount) {
        if (paymentAmount >= amount) {
            this.isPaid = true;
            this.paymentDate = LocalDate.now();
        } else {
            this.amount -= paymentAmount; // Ana para sadece ödeme yapıldığında azalır
        }
    }
    public String getStatus() {
        if (isPaid()) {
            return "Paid"; // Fatura ödendiyse
        } else if (isOverdue()) {
            return "Overdue"; // Fatura vadesini geçtiyse
        } else {
            return "Pending"; // Fatura beklemede (ödenmemiş ama vadesi geçmemiş)
        }
    }
    public long daysUntilDue() {
        if (isPaid()) {
            return 0; // Eğer fatura ödenmişse 0 gün kalmış olur
        }
        return LocalDate.now().until(dueDate).getDays(); // Vade tarihine kadar kalan gün sayısı
    }

}
