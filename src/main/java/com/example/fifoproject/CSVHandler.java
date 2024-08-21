package com.example.fifoproject;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CSVHandler {

    private static final String CSV_FILE_PATH = "customers.csv";
    // CSV dosyasına müşteri ve faturaları kaydetme
    public static void saveCustomersToCSV(List<Customer> customers) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE_PATH))) {
            for (Customer customer : customers) {
                // Eğer müşterinin faturası yoksa fatura kısımları boş olacak
                if (customer.getInvoices().isEmpty()) {
                    String line = String.format("%s,%s,%s,null,null,null",
                            customer.getId(),
                            customer.getName(),
                            customer.getLastName()
                    );
                    writer.println(line);
                } else {
                    for (Invoice invoice : customer.getInvoices()) {
                        String line = String.format("%s,%s,%s,%f,%s,%b",
                                customer.getId(),
                                customer.getName(),
                                customer.getLastName(),
                                invoice.getAmount(),
                                invoice.getDueDate(),
                                invoice.isPaid()
                        );
                        writer.println(line);
                    }
                }
            }
            System.out.println("CSV dosyasına başarıyla yazıldı.");
        } catch (IOException e) {
            System.out.println("CSV dosyasına yazılırken hata oluştu: " + e.getMessage());
        }
    }

    // CSV dosyasından müşteri ve faturaları okuma
    public static List<Customer> loadCustomersFromCSV() {
        List<Customer> customers = new ArrayList<>();

        File csvFile = new File(CSV_FILE_PATH);
        if (!csvFile.exists()) {
            System.out.println("CSV dosyası bulunamadı. Yeni bir dosya oluşturulacak.");
            try {
                csvFile.createNewFile();
            } catch (IOException e) {
                System.out.println("CSV dosyası oluşturulurken hata oluştu: " + e.getMessage());
            }
            return customers; // Eğer dosya yoksa boş müşteri listesi döner
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                // CSV'deki veriyi müşteri ve fatura bilgisine dönüştürme
                String id = parts[0];
                String name = parts[1];
                String lastName = parts[2];
                double amount = Double.parseDouble(parts[3]);
                LocalDate dueDate = LocalDate.parse(parts[4]);
                boolean isPaid = Boolean.parseBoolean(parts[6]);

                Customer customer = findCustomerById(customers, id);
                if (customer == null) {
                    customer = new Customer(name, lastName);
                    customer.setId(id);
                    customers.add(customer);
                }

                Invoice invoice = new Invoice(amount, dueDate);
                if (isPaid) {
                    invoice.makePayment(amount);
                }
                customer.addInvoice(invoice);
            }
        } catch (IOException e) {
            System.out.println("CSV dosyasından okunurken hata oluştu: " + e.getMessage());
        }

        return customers;
    }

    // CSV'den yüklerken müşteri id'si ile kontrol
    private static Customer findCustomerById(List<Customer> customers, String id) {
        for (Customer customer : customers) {
            if (customer.getId().equals(id)) {
                return customer;
            }
        }
        return null;
    }
}
