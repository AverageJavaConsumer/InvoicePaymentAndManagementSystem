package com.example.fifoproject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Main {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Veritabanı bağlantısını kur ve gerekli tabloları oluştur
        DatabaseManager.connect();

        boolean running = true;

        while (running) {
            try {
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
                        viewAllInvoices();
                        break;
                    case 8:
                        deleteCustomer();
                        break;
                    case 9:
                        viewPaymentLogs(); // Ödeme Loglarını Görüntüle
                        break;
                    case 10:
                        displayCustomerPaymentSummary(); // Ödeme-Fatura İlişkilerini Görüntüle
                        break;
                    case 0:
                        running = false; // Programdan çık
                        break;
                    default:
                        System.out.println("Geçersiz seçim, lütfen tekrar deneyin.");
                }
            } catch (Exception e) {
                System.out.println("Beklenmeyen bir hata oluştu: " + e.getMessage());
                scanner.nextLine(); // Hatalı girdi sonrası scanner'ı temizlemek için
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
        System.out.println("7. Tüm Faturaları Görüntüle");
        System.out.println("8. Müşteri Sil");
        System.out.println("9. Ödeme Loglarını Görüntüle");
        System.out.println("10. Ödeme-Fatura İlişkilerini Görüntüle");
        System.out.println("0. Çıkış");
        System.out.print("Seçiminizi yapın: ");
    }


    private static void addCustomer() {
        try {
            System.out.print("Müşteri adı: ");
            String firstName = scanner.nextLine();
            System.out.print("Müşteri soyadı: ");
            String lastName = scanner.nextLine();

            // Grup Şirketi sorusunu güncelliyoruz
            System.out.print("Müşteri Oyak Gruba Bağlı mı? (Evet/Hayır): ");
            String groupInput = scanner.nextLine().trim().toLowerCase();
            boolean isGroupCompany = groupInput.equals("evet");

            // Müşteri ekle ve ID'sini al
            String customerId = DatabaseManager.insertCustomer(firstName, lastName, isGroupCompany);

            System.out.println(firstName + " " + lastName + " başarıyla eklendi. " + "Grup şirketlerinden mi? " + isGroupCompany + " ID: " + customerId);

        } catch (Exception e) {
            System.out.println("Müşteri eklenirken bir hata oluştu: " + e.getMessage());
        }
    }


    private static void deleteCustomer() {
        try {
            System.out.print("Müşteri ID: ");
            String id = scanner.nextLine();

            // Veritabanından müşteri sil
            DatabaseManager.deleteCustomer(id);
            System.out.println("Müşteri başarıyla silindi.");

        } catch (Exception e) {
            System.out.println("Müşteri silinirken bir hata oluştu: " + e.getMessage());
        }
    }

    private static void addInvoiceToCustomer() {
        try {
            System.out.print("Müşteri ID: ");
            String customerId = scanner.nextLine();

            System.out.print("Fatura tutarı: ");
            double amount = Double.parseDouble(scanner.nextLine());

            LocalDate dueDate = null;
            boolean validDate = false;

            // Kullanıcıdan doğru formatta tarih alınana kadar devam et
            while (!validDate) {
                System.out.print("Vade tarihi (YYYY-MM-DD): ");
                String dueDateStr = scanner.nextLine();
                try {
                    dueDate = LocalDate.parse(dueDateStr);
                    validDate = true; // Tarih doğru formatta girildi
                } catch (DateTimeParseException e) {
                    System.out.println("Geçersiz tarih formatı. Lütfen YYYY-MM-DD formatında bir tarih girin.");
                }
            }

            // Veritabanına fatura ekle ve fazla ödeme durumunu kontrol et
            DatabaseManager.insertInvoice(customerId, amount, dueDate.toString());

        } catch (NumberFormatException e) {
            System.out.println("Geçersiz fatura tutarı. Lütfen geçerli bir sayı girin.");
        } catch (Exception e) {
            System.out.println("Fatura eklenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private static void makePayment() {
        try {
            System.out.print("Müşteri ID: ");
            String customerId = scanner.nextLine();

            System.out.print("Ödeme tutarı: ");
            double paymentAmount = scanner.nextDouble();
            scanner.nextLine(); // nextDouble'dan sonra satır sonunu temizlemek için

            // Veritabanında ödeme işlemi
            double remainingAmount = DatabaseManager.makePayment(customerId, paymentAmount); // Üçüncü parametre kaldırıldı
            System.out.println("Ödeme yapıldı: " + paymentAmount);

            if (remainingAmount > 0) {
                // Kalan miktarı müşterinin fazla ödemesine ekle
                Customer customer = DatabaseManager.getCustomerById(customerId);
                customer.addExtraPayment(remainingAmount);
                System.out.println("Artan miktar müşterinin fazla ödemesi olarak kaydedildi: " + remainingAmount);
            }

        } catch (Exception e) {
            System.out.println("Ödeme yapılırken bir hata oluştu: " + e.getMessage());
        }
    }



    private static void viewCustomerDebt() {
        try {
            System.out.print("Müşteri ID: ");
            String customerId = scanner.nextLine();

            System.out.println("Toplam Borcu mu görmek istersiniz yoksa belirli bir fatura borcunu mu?");
            System.out.println("1. Toplam Borç");
            System.out.println("2. Belirli Fatura Borcu");
            int choice = scanner.nextInt();
            scanner.nextLine(); // nextInt sonrası scanner'ı temizlemek için

            if (choice == 1) {
                // Toplam borcu göster
                double totalDebt = DatabaseManager.getCustomerDebt(customerId);
                System.out.println("Toplam Borç: " + totalDebt);
            } else if (choice == 2) {
                // Belirli fatura borcunu ve ödenmiş durumunu göster
                System.out.print("Fatura ID: ");
                int invoiceId = scanner.nextInt();
                String invoiceDetails = DatabaseManager.getInvoiceDebt(invoiceId);
                System.out.println(invoiceDetails);
            } else {
                System.out.println("Geçersiz seçim.");
            }

        } catch (Exception e) {
            System.out.println("Müşteri borcunu görüntülerken bir hata oluştu: " + e.getMessage());
        }
    }



    private static void viewDaysUntilDue() {
        try {
            System.out.print("Müşteri ID: ");
            String customerId = scanner.nextLine();

            // Müşterinin tüm faturalarının vadeye kalan günlerini göster
            DatabaseManager.getDaysUntilDue(customerId);

        } catch (Exception e) {
            System.out.println("Vadeye kalan günler görüntülenirken bir hata oluştu: " + e.getMessage());
        }
    }


    private static void viewLateFee() {
        try {
            System.out.print("Müşteri ID: ");
            String customerId = scanner.nextLine();

            System.out.println("Toplam Gecikme Faizini mi görmek istersiniz yoksa belirli bir fatura gecikme faizini mi?");
            System.out.println("1. Toplam Gecikme Faizi");
            System.out.println("2. Belirli Fatura Gecikme Faizi");
            int choice = scanner.nextInt();
            scanner.nextLine(); // nextInt sonrası scanner'ı temizlemek için

            if (choice == 1) {
                // Tüm faturaların toplam gecikme faizini göster
                double totalLateFee = DatabaseManager.getLateFee(customerId);
                System.out.println("Toplam Gecikme Faizi: " + totalLateFee);
            } else if (choice == 2) {
                // Belirli fatura gecikme faizini göster
                System.out.print("Fatura ID: ");
                int invoiceId = scanner.nextInt();
                Double lateFee = DatabaseManager.getInvoiceLateFee(invoiceId);
                System.out.println(lateFee);
            } else {
                System.out.println("Geçersiz seçim.");
            }

        } catch (Exception e) {
            System.out.println("Gecikme faizi görüntülenirken bir hata oluştu: " + e.getMessage());
        }
    }




    private static void viewAllInvoices() {
        try {
            // Tüm müşterilerin faturalarını veritabanından görüntüle
            DatabaseManager.viewAllInvoices();

        } catch (Exception e) {
            System.out.println("Faturalar görüntülenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private static void viewPaymentLogs() {
        try {
            // Tüm ödeme loglarını veritabanından görüntüle
            DatabaseManager.viewAllPaymentLogs();

        } catch (Exception e) {
            System.out.println("Ödeme logları görüntülenirken bir hata oluştu: " + e.getMessage());
        }
    }

    private static void displayCustomerPaymentSummary() {
        try {
            System.out.print("Müşteri ID'sini girin: ");
            String customerId = scanner.nextLine();  // Müşteri ID'sini al

            // Müşteri ödeme özetini görüntüle
            DatabaseManager.displayCustomerPaymentSummary(customerId);

        } catch (Exception e) {
            System.out.println("Fatura-Ödeme ilişkileri görüntülenirken bir hata oluştu: " + e.getMessage());
        }
    }


}
