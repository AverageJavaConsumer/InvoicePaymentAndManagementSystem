package com.example.fifoproject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static DebtManager debtManager = new DebtManager();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        List<Customer> customers = CSVHandler.loadCustomersFromCSV();

        // Müşterileri sırayla debtManager'a ekle
        for (Customer customer : customers) {
            debtManager.addCustomer(customer);
        }

        boolean running = true;

        while (running) {
            printMenu();
            int choice = scanner.nextInt();
            scanner.nextLine(); // nextInt()'in ardından satır sonunu temizlemek için kullanılır.

            switch (choice) {
                case 1:
                    addCustomer();
                    break;
                case 2:
                    addInvoiceToCustomer();
                    break;
                case 3:
                    makePayment();
                    break;
                case 4:
                    viewCustomerDebt();
                    break;
                case 5:
                    viewDaysUntilDue();
                    break;
                case 6:
                    viewLateFee();
                    break;
                case 7:
                    makeBulkPayment();
                    break;
                case 8:
                    processNextCustomer();
                    break;
                case 9:
                    viewAllInvoices();
                    break;
                case 10:
                    deleteCustomer();
                    break;
                case 0:
                    running = false; // Programdan çık
                    break;
                default:
                    System.out.println("Geçersiz seçim, lütfen tekrar deneyin.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Borç Yönetim Sistemi ---");
        System.out.println("1. Müşteri Ekle");
        System.out.println("2. Müşteriye Fatura Ekle");
        System.out.println("3. Ödeme Yap");
        System.out.println("4. Müşteri Borcunu Görüntüle");
        System.out.println("5. Vadeye Kalan Günleri Görüntüle");
        System.out.println("6. Gecikme Faizini Görüntüle");
        System.out.println("7. Toplu Ödeme Yap");
        System.out.println("8. Sıradaki Müşteriyi İşle");
        System.out.println("9. Tüm Faturaları Görüntüle");
        System.out.println("10. Müşteri Sil");
        System.out.println("0. Çıkış");
        System.out.print("Seçiminizi yapın: ");
    }

    private static void addCustomer() {
        try {
            System.out.print("Müşteri adı: ");
            String firstName = scanner.nextLine();
            System.out.print("Müşteri soyadı: ");
            String lastName = scanner.nextLine();

            Customer customer = new Customer(firstName, lastName);
            debtManager.addCustomer(customer);
            System.out.println(firstName + " " + lastName + " başarıyla eklendi. ID: " + customer.getId());

            // Müşteri eklendikten sonra CSV'yi güncelle
            CSVHandler.saveCustomersToCSV(debtManager.getCustomerQueue().stream().toList());

        } catch (Exception e) {
            System.out.println("Müşteri eklenirken bir hata oluştu: " + e.getMessage());
        }
    }


    private static void deleteCustomer() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            debtManager.getCustomerQueue().remove(customer);
            System.out.println("Müşteri başarıyla silindi.");

            // Müşteri silindikten sonra CSV'yi güncelle
            CSVHandler.saveCustomersToCSV(debtManager.getCustomerQueue().stream().toList());

        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }

    private static void addInvoiceToCustomer() {
        try {
            System.out.print("Müşteri ID: ");
            String id = scanner.nextLine();
            Customer customer = findCustomerById(id);

            if (customer != null) {
                System.out.print("Fatura tutarı: ");
                double amount = Double.parseDouble(scanner.nextLine());

                System.out.print("Vade tarihi (YYYY-MM-DD): ");
                String dueDateStr = scanner.nextLine();
                LocalDate dueDate = LocalDate.parse(dueDateStr);

                Invoice invoice = new Invoice(amount, dueDate);
                customer.addInvoice(invoice);

                System.out.println("Fatura başarıyla eklendi.");

                // Fatura eklendikten sonra CSV'yi güncelle
                CSVHandler.saveCustomersToCSV(debtManager.getCustomerQueue().stream().toList());

            } else {
                System.out.println("Müşteri bulunamadı.");
            }
        } catch (DateTimeParseException e) {
            System.out.println("Geçersiz tarih formatı. Lütfen YYYY-MM-DD formatında bir tarih girin.");
        } catch (NumberFormatException e) {
            System.out.println("Geçersiz fatura tutarı. Lütfen geçerli bir sayı girin.");
        } catch (Exception e) {
            System.out.println("Fatura eklenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private static void makePayment() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            System.out.print("Ödeme tutarı: ");
            double paymentAmount = scanner.nextDouble();

            for (Invoice invoice : customer.getInvoices()) {
                if (!invoice.isPaid()) {
                    invoice.makePayment(paymentAmount);
                    System.out.println("Ödeme yapıldı: " + paymentAmount);
                    break; // İlk ödenmemiş faturada dururuz
                }
            }
        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }

    private static void viewCustomerDebt() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            customer.displayDebtDetails(); // Müşteri borç detaylarını göster
        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }


    private static void viewDaysUntilDue() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            Invoice priorityInvoice = debtManager.getHighestPriorityInvoice(customer);
            if (priorityInvoice != null) {
                System.out.println("Vadeye kalan günler: " + priorityInvoice.daysUntilDue());
            } else {
                System.out.println("Ödenmemiş fatura bulunamadı.");
            }
        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }

    private static void viewLateFee() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            Invoice priorityInvoice = debtManager.getHighestPriorityInvoice(customer);
            if (priorityInvoice != null && priorityInvoice.isOverdue()) {
                System.out.println("Gecikme faizi: " + priorityInvoice.calculateLateFee());
            } else {
                System.out.println("Gecikmiş fatura bulunamadı.");
            }
        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }

    private static void makeBulkPayment() {
        System.out.print("Müşteri ID: ");
        String id = scanner.nextLine();
        Customer customer = findCustomerById(id);

        if (customer != null) {
            System.out.print("Toplu ödeme tutarı: ");
            double paymentAmount = scanner.nextDouble();

            customer.makeBulkPayment(paymentAmount);
            System.out.println("Toplu ödeme yapıldı.");
        } else {
            System.out.println("Müşteri bulunamadı.");
        }
    }

    private static void processNextCustomer() {
        if (debtManager.hasCustomersToProcess()) {
            debtManager.processNextCustomer();
        } else {
            System.out.println("İşlenecek müşteri yok.");
        }
    }

    // Müşteri ID'ye göre bul
    private static Customer findCustomerById(String id) {
        for (Customer customer : debtManager.getCustomerQueue()) {
            if (customer.getId().equals(id)) {
                return customer;
            }
        }
        return null;
    }

    private static void viewAllInvoices() {
        List<InvoiceInfo> allInvoices = new ArrayList<>();

        // Tüm müşterilerin faturalarını topla
        for (Customer customer : debtManager.getCustomerQueue()) {
            for (Invoice invoice : customer.getInvoices()) {
                if (!invoice.isPaid()) {
                    allInvoices.add(new InvoiceInfo(customer.getName(), invoice));
                }
            }
        }

        // Gecikme günü en fazla olanı en üste alacak şekilde sırala
        allInvoices.sort((i1, i2) -> {
            if (i1.invoice.isOverdue() && i2.invoice.isOverdue()) {
                return Long.compare(i2.invoice.daysUntilDue(), i1.invoice.daysUntilDue());
            } else if (i1.invoice.isOverdue()) {
                return -1;
            } else if (i2.invoice.isOverdue()) {
                return 1;
            } else {
                return Double.compare(i2.invoice.getAmount(), i1.invoice.getAmount());
            }
        });

        // Faturaları yazdır
        System.out.println("\n--- Tüm Faturalar ---");
        for (InvoiceInfo info : allInvoices) {
            System.out.println("Müşteri Adı: " + info.customerName);
            System.out.println("Fatura Tutarı: " + info.invoice.getAmount());
            System.out.println("Vade Tarihi: " + info.invoice.getDueDate());
            System.out.println("Gecikme Durumu: " + (info.invoice.isOverdue() ? "Evet" : "Hayır"));
            if (info.invoice.isOverdue()) {
                System.out.println("Gecikme Gün Sayısı: " + Math.abs(info.invoice.daysUntilDue()));
            }
            System.out.println("---------------------");
        }
    }

    // Yardımcı sınıf (Müşteri adıyla birlikte faturayı tutar)
    private static class InvoiceInfo {
        String customerName;
        Invoice invoice;

        InvoiceInfo(String customerName, Invoice invoice) {
            this.customerName = customerName;
            this.invoice = invoice;
        }
    }
}