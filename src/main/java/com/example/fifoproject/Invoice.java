package com.example.fifoproject;

import java.time.LocalDate;

public class Invoice {
    private double amount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private boolean isPaid;
    private static final double INTEREST_RATE = 0.02;

    public Invoice(double amount, LocalDate dueDate) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = false;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public double calculateLateFee() {
        if (isOverdue()) {
            long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
            return overdueDays * INTEREST_RATE * amount;
        }
        return 0;
    }

    public double getTotalDebtWithLateFee() {
        return amount + calculateLateFee();
    }

    public boolean isOverdue() {
        return !isPaid && LocalDate.now().isAfter(dueDate);
    }

    public void makePayment(double paymentAmount) {
        if (paymentAmount >= amount) {
            this.isPaid = true;
            this.paymentDate = LocalDate.now();
        } else {
            this.amount -= paymentAmount;
            this.paymentDate = LocalDate.now();
        }
    }

    public String getStatus() {
        if (isPaid()) {
            return "Paid";
        } else if (isOverdue()) {
            return "Overdue";
        } else {
            return "Pending";
        }
    }

    public long daysUntilDue() {
        if (isPaid()) {
            return 0;
        }
        return LocalDate.now().until(dueDate).getDays();
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}
