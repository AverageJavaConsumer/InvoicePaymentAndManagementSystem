package com.example.fifoproject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Customer {
    private String name;
    private String LastName;
    private String id;
    private List<Invoice> invoices; // Müşterinin faturaları

    public Customer(String name, String LastName) {
        this.name = name;
        this.LastName = LastName;
        this.id = UUID.randomUUID().toString(); // Rastgele benzersiz ID
        this.invoices = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
    }

    // Müşterinin borç detaylarını ayrı ayrı göster
    public void displayDebtDetails() {
        double totalPrincipal = 0;
        double totalLateFees = 0;

        for (Invoice invoice : invoices) {
            if (!invoice.isPaid()) {
                totalPrincipal += invoice.getAmount();
                totalLateFees += invoice.calculateLateFee();
            }
        }

        System.out.println("Ana Para: " + totalPrincipal);
        System.out.println("Gecikme Faizi: " + totalLateFees);
        System.out.println("Toplam Borç (Ana Para + Gecikme Faizi): " + (totalPrincipal + totalLateFees));
    }

    // Müşterinin toplam borcunu hesapla
    public double getTotalDebt() {
        return invoices.stream()
                .filter(invoice -> !invoice.isPaid())
                .mapToDouble(Invoice::getTotalDebtWithLateFee)
                .sum();
    }

    // Müşterinin gecikmiş faturalarını bul
    public List<Invoice> getOverdueInvoices() {
        List<Invoice> overdueInvoices = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (invoice.isOverdue()) {
                overdueInvoices.add(invoice);
            }
        }
        return overdueInvoices;
    }

    // Müşterinin ödemesi gereken toplam gecikmiş borcunu hesapla
    public double getTotalOverdueDebt() {
        return getOverdueInvoices().stream()
                .mapToDouble(Invoice::getTotalDebtWithLateFee)
                .sum();
    }

    // Toplu ödeme
    public void makeBulkPayment(double totalPayment) {
        for (Invoice invoice : getInvoices()) {
            if (!invoice.isPaid()) {
                if (totalPayment >= invoice.getAmount()) {
                    totalPayment -= invoice.getAmount();
                    invoice.makePayment(invoice.getAmount());
                } else {
                    invoice.makePayment(totalPayment);
                    break; // Ödeme bittiyse döngüden çık
                }
            }
        }
    }
}
