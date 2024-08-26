package com.example.fifoproject;

public class InterestRateCalculator {

    public static double InterestCalculation(boolean isGroupCompany, long overdueDays, boolean isEarlyPayment) {
        double baseRate;
        if (isGroupCompany) {
            baseRate = 0.01;  // Grup şirketleri için düşük faiz oranı
        } else {
            baseRate = 0.02;  // Diğer şirketler için standart faiz oranı
        }
        double adjustment = 0;

        if (isEarlyPayment) {
            adjustment = -0.005; // Erken ödeme için faiz indirimi
        } else if (overdueDays > 0) {
            adjustment = 0.001 * overdueDays; // Gecikmeye bağlı faiz artışı
        }

        return baseRate + adjustment;
    }
}
