package com.example.fifoproject;

public class Company {
    private String id;
    private String name;
    private double companyBalance; // Şirket bütçesi

    public Company(String name) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.companyBalance = 0;
    }

    public void addToBudget(double amount) {
        this.companyBalance += amount; // Fazla ödenen miktar şirkete eklenir
        System.out.println("Fazla ödeme faize konuldu, yeni bütçe: " + companyBalance);
    }
    public void applyInterestToBudget() {
        // Faiz eklemek için bir faiz oranı belirliyoruz, örneğin %5
        double interestRate = 0.05;
        double interest = companyBalance * interestRate;
        companyBalance += interest;
        System.out.println("Faiz eklendi: " + interest + ", Yeni bütçe: " + companyBalance);
    }


    public void deductFromCompanyBalance(double amount) {
        if (companyBalance >= amount) {
            companyBalance -= amount;
            System.out.println("Bütçeden düşülen tutar: " + amount + ", Kalan bütçe: " + companyBalance);
        } else {
            System.out.println("Bütçede yeterli miktar yok.");
        }
    }
    public double getCompanyBalance() {
        return companyBalance;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
