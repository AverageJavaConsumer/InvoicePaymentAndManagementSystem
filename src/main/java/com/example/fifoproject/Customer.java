package com.example.fifoproject;

import java.util.UUID;

public class Customer {
    private String id;
    private String name;
    private String lastName;
    private boolean isGroupCompany; // Grup şirket kontrolü
    private double extraPayment;  // Fazla ödeme kaydı

    public Customer(String name, String lastName) {
        this.name = name;
        this.lastName = lastName;
        this.isGroupCompany = false; // Varsayılan olarak grup şirket değil
        this.id = UUID.randomUUID().toString(); // Rastgele benzersiz ID
        this.extraPayment = 0; // Başlangıçta fazla ödeme yok
    }

    // Müşteri ID'sini veritabanında ayarlamak için constructor
    public Customer(String id, String name, String lastName, boolean isGroupCompany) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.isGroupCompany = isGroupCompany;
        this.extraPayment = 0; // Başlangıçta fazla ödeme yok
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGroupCompany() {
        return isGroupCompany;
    }

    public void setGroupCompany(boolean groupCompany) {
        isGroupCompany = groupCompany;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getExtraPayment() {
        return extraPayment;
    }

    // Fazla ödeme ekleme
    public void addExtraPayment(double amount) {
        this.extraPayment += amount;
        System.out.println("Fazla ödeme eklendi: " + amount + ". Toplam fazla ödeme: " + this.extraPayment);
    }

    // Fazla ödemeden düşme
    public void deductFromExtraPayment(double amount) {
        if (extraPayment >= amount) {
            extraPayment -= amount;
            System.out.println("Fazla ödemeden düşülen miktar: " + amount + ". Kalan fazla ödeme: " + this.extraPayment);
        } else {
            System.out.println("Yeterli fazla ödeme yok.");
        }
    }

    // Müşterinin toplam borcunu hesaplar (Veritabanından)
    public double getTotalDebt() {
        return DatabaseManager.getCustomerDebt(id);
    }

    // Müşterinin gecikmiş borçlarını hesaplar (Veritabanından)
    public double getTotalOverdueDebt() {
        try {
            return DatabaseManager.getLateFee(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Müşteri borç detaylarını göster
    public void displayDebtDetails() {
        double totalPrincipal = getTotalDebt();
        double totalLateFees = getTotalOverdueDebt();

        System.out.println("Ana Para: " + totalPrincipal);
        System.out.println("Gecikme Faizi: " + totalLateFees);
        System.out.println("Toplam Borç (Ana Para + Gecikme Faizi): " + (totalPrincipal + totalLateFees));
    }

    // Toplu ödeme yapma işlemi (Veritabanı üzerinden)
    public void makeBulkPayment(double totalPayment) {
        DatabaseManager.makeBulkPayment(id, totalPayment);
    }
}
