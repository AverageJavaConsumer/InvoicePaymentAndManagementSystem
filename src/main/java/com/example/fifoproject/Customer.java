package com.example.fifoproject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

public class Customer {
    private String id;
    private String name;
    private String lastName;
    private boolean isGroupCompany; //Grup şirket kontrolü

    public Customer(String name, String lastName) {
        this.name = name;
        this.lastName = lastName;
        this.isGroupCompany = isGroupCompany;
        this.id = UUID.randomUUID().toString(); // Rastgele benzersiz ID

    }

    // Müşteri ID'sini veritabanında ayarlamak için constructor
    public Customer(String id, String name, String lastName, boolean isGroupCompany) {
        this.id = id;
        this.name = name;
        this.lastName = lastName;
        this.isGroupCompany = isGroupCompany;
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

    // Müşterinin toplam borcunu hesaplar (Veritabanından)
    public double getTotalDebt() {
        return DatabaseManager.getCustomerDebt(id);
    }

    // Müşterinin gecikmiş borçlarını hesaplar (Veritabanından)
    public double getTotalOverdueDebt() {
        try {
            return DatabaseManager.getLateFee(id);
        } catch (SQLException e) {
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
