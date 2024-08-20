package main.java.com.example.fifoproject;

import java.util.LinkedList;
import java.util.Queue;

public class DebtManager {
    private Queue<Customer> customerQueue; // Müşterilerin tutulduğu FIFO kuyruğu

    public DebtManager() {
        this.customerQueue = new LinkedList<>();
    }

    // Müşteri ekleme


    // En üstteki müşteriyi kontrol et (FIFO mantığıyla)
    public Customer getNextCustomer() {
        return customerQueue.peek();
    }

    // En üstteki müşteriyi çıkar (FIFO mantığıyla)
    public void processNextCustomer() {
        Customer customer = customerQueue.poll();
        if (customer != null) {
            processCustomerInvoices(customer);
        }
    }

    // Müşterinin faturalarını işle
    private void processCustomerInvoices(Customer customer) {
        System.out.println("Processing invoices for customers: " + customer.getName());

        // Müşterinin ödenmemiş faturalarını sırayla işle
        for (Invoice invoice : customer.getInvoices()) {
            if (!invoice.isPaid()) {
                System.out.println("Invoice Amount: " + invoice.getAmount());
                System.out.println("Invoice Due Date: " + invoice.getDueDate());
                System.out.println("Invoice Status: " + invoice.getStatus()); // Fatura durumunu göster
            }
        }

        // Müşterinin toplam borcunu ve gecikmiş borcunu yazdır
        System.out.println("Total Debt: " + customer.getTotalDebt());
        System.out.println("Total Overdue Debt: " + customer.getTotalOverdueDebt());
    }


    // Kuyrukta işlem yapılacak müşteri olup olmadığını kontrol et
    public boolean hasCustomersToProcess() {
        return !customerQueue.isEmpty();
    }
    public Queue<Customer> getCustomerQueue() {
        return customerQueue;
    }


    // Müşteri ekleme
    public void addCustomer(Customer customer) {
        customerQueue.add(customer);
    }

    // Öncelikli fatura bulma
    public Invoice getHighestPriorityInvoice(Customer customer) {
        return customer.getInvoices().stream()
                .filter(invoice -> !invoice.isPaid())
                .sorted((i1, i2) -> {
                    if (i1.isOverdue() && !i2.isOverdue()) {
                        return -1;
                    } else if (!i1.isOverdue() && i2.isOverdue()) {
                        return 1;
                    } else {
                        return Double.compare(i2.getAmount(), i1.getAmount());
                    }
                })
                .findFirst()
                .orElse(null);
    }
}

