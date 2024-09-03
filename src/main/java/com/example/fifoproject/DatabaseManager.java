package com.example.fifoproject;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class DatabaseManager {

    private static Connection connection;

    // Veritabanına bağlanma
    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = "C:/Users/Acer/Desktop/customers.db";
            File dbFile = new File(dbPath);

            // Eğer veritabanı dosyası mevcutsa, yeniden oluşturmadan ona bağlan
            if (dbFile.exists()) {
                System.out.println("Veritabanı dosyası bulundu: " + dbFile.getAbsolutePath());
            } else {
                System.out.println("Veritabanı dosyası mevcut değil, yeni oluşturulacak.");
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            System.out.println("Veritabanına başarıyla bağlandı.");

            // Veritabanı dosyasının oluşturulup oluşturulmadığını kontrol et
            if (!dbFile.exists()) {
                System.out.println("Veritabanı dosyası mevcut değil, yeni oluşturulacak.");
            } else {
                System.out.println("Veritabanı dosyası bulundu: " + dbFile.getAbsolutePath());
            }

            // Eğer tablolar zaten mevcut değilse, oluştur
            createTables();

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Veritabanına bağlanırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Müşteri ve Fatura tablolarını oluştur
    private static void createTables() {
        String createCustomerTable = "CREATE TABLE IF NOT EXISTS Customer (" +
                "id TEXT PRIMARY KEY," +
                "firstName TEXT," +
                "lastName TEXT," +
                "isGroupCompany BOOLEAN," +
                "excessPayment REAL DEFAULT 0" +
                ");";

        String createInvoiceTable = "CREATE TABLE IF NOT EXISTS Invoice (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customerId TEXT, " +
                "amount REAL, " +
                "lateFee REAL DEFAULT 0, " +
                "totalDebt REAL DEFAULT 0, " +
                "dueDate TEXT, " +
                "paymentDate TEXT, " +
                "isPaid INTEGER, " +
                "FOREIGN KEY (customerId) REFERENCES Customer(id)" +
                ");";

        String createPaymentLogTable = "CREATE TABLE IF NOT EXISTS PaymentLog (" +
                "paymentId TEXT, " +
                "invoiceId INTEGER, " +
                "customerId TEXT, " +
                "paidAmount REAL, " +
                "paymentDate TEXT, " +
                "PRIMARY KEY (paymentId, invoiceId), " + // Bu satırı güncelledik
                "FOREIGN KEY (invoiceId) REFERENCES Invoice(id), " +
                "FOREIGN KEY (customerId) REFERENCES Customer(id)" +
                ");";


        String createPaymentInvoiceRelationTable = "CREATE TABLE IF NOT EXISTS PaymentInvoiceRelation (" +
                "paymentId TEXT, " +
                "invoiceId INTEGER, " +
                "FOREIGN KEY (paymentId) REFERENCES PaymentLog(paymentId), " +
                "FOREIGN KEY (invoiceId) REFERENCES Invoice(id)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCustomerTable);
            stmt.execute(createInvoiceTable);
            stmt.execute(createPaymentLogTable);  // invoiceId sütunu burada tanımlanmalı
            stmt.execute(createPaymentInvoiceRelationTable);
            System.out.println("Tablolar başarıyla oluşturuldu.");
        } catch (SQLException e) {
            System.out.println("Tablolar oluşturulurken hata oluştu: " + e.getMessage());
        }
    }


    // Müşteri ekleme
    public static String insertCustomer(String firstName, String lastName, boolean isGroupCompany) {
        String insertCustomerSQL = "INSERT INTO Customer (id, firstName, lastName, isGroupCompany) VALUES (?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString(); // Benzersiz müşteri ID'si

        try (PreparedStatement pstmt = connection.prepareStatement(insertCustomerSQL)) {
            pstmt.setString(1, id);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setBoolean(4, isGroupCompany);
            pstmt.executeUpdate();
            System.out.println("Müşteri başarıyla eklendi.");
        } catch (SQLException e) {
            System.out.println("Müşteri eklenirken hata oluştu: " + e.getMessage());
        }

        return id; // Müşteri ID'sini döndür
    }

    // Müşteri silme
    public static void deleteCustomer(String id) {
        String deleteCustomerSQL = "DELETE FROM Customer WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteCustomerSQL)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            System.out.println("Müşteri başarıyla silindi.");

        } catch (SQLException e) {
            System.out.println("Müşteri silinirken hata oluştu: " + e.getMessage());
        }
    }

    // Fatura ekleme
    public static void insertInvoice(String customerId, double amount, String dueDate) {
        String insertInvoiceSQL = "INSERT INTO Invoice (customerId, amount, dueDate, totalDebt, isPaid) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertInvoiceSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customerId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, dueDate);

            // Yeni faturayı eklerken fazla ödeme durumu kontrol ediliyor ama uygulanmıyor
            double excessPayment = getExcessPayment(customerId);
            boolean invoicePaid = false;
            double totalDebt = amount;

            if (excessPayment > 0) {
                System.out.println("Müşterinin mevcut fazla ödemesi var: " + excessPayment);
                // Otomatik düşme yapılmıyor
            }

            pstmt.setDouble(4, totalDebt);
            pstmt.setInt(5, invoicePaid ? 1 : 0);

            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int invoiceId = generatedKeys.getInt(1);
                System.out.println("Fatura başarıyla eklendi, Fatura ID: " + invoiceId);
            } else {
                System.out.println("Fatura eklendi ancak ID alınamadı.");
            }

        } catch (SQLException e) {
            System.out.println("Fatura eklenirken hata oluştu: " + e.getMessage());
        }
    }


    // Fazla ödemeyi getir
    public static double getExcessPayment(String customerId) {
        String query = "SELECT excessPayment FROM Customer WHERE id = ?";
        double excessPayment = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                excessPayment = rs.getDouble("excessPayment");
            }
        } catch (SQLException e) {
            System.out.println("Fazla ödeme sorgulanırken hata oluştu: " + e.getMessage());
        }

        return excessPayment;
    }

    // Fazla ödemeyi güncelle
    public static void updateExcessPayment(String customerId, double newExcessPayment) {
        String updateSQL = "UPDATE Customer SET excessPayment = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setDouble(1, newExcessPayment);
            pstmt.setString(2, customerId);
            pstmt.executeUpdate();
            System.out.println("Fazla ödeme güncellendi: " + newExcessPayment);
        } catch (SQLException e) {
            System.out.println("Fazla ödeme güncellenirken hata oluştu: " + e.getMessage());
        }
    }

    // Ödeme yapma işlemi
    public static double makePayment(String customerId, double paymentAmount) {
        Customer customer = getCustomerById(customerId);

        if (customer == null) {
            System.out.println("Müşteri bulunamadı.");
            return paymentAmount;
        }

        String selectUnpaidInvoiceSQL = "SELECT * FROM Invoice WHERE customerId = ? AND isPaid = 0 ORDER BY dueDate ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(selectUnpaidInvoiceSQL)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            String paymentId = UUID.randomUUID().toString(); // Tek bir paymentId üretelim

            while (rs.next() && paymentAmount > 0) {
                double invoiceAmount = rs.getDouble("totalDebt");
                int invoiceId = rs.getInt("id");

                System.out.println("Fatura ID: " + invoiceId + ", Toplam Borç: " + invoiceAmount);

                if (paymentAmount >= invoiceAmount && invoiceAmount > 0) {
                    // Fatura tamamen ödeniyor
                    paymentAmount -= invoiceAmount;
                    updateInvoiceAsPaid(invoiceId); // isPaid alanını ve totalDebt alanını güncelle

                    // Fatura ve ödeme ilişkisini kaydet
                    insertPaymentLog(paymentId, customerId, invoiceAmount, invoiceId);
                    insertPaymentInvoiceRelation(paymentId, invoiceId);

                    System.out.println("Fatura " + invoiceId + " tamamen ödendi.");
                } else if (paymentAmount > 0 && paymentAmount < invoiceAmount) {
                    // Kısmi ödeme
                    double remainingDebt = invoiceAmount - paymentAmount;

                    // totalDebt üzerinde işlem yapıyoruz
                    updateInvoiceAmount(invoiceId, remainingDebt);

                    // Fatura ve ödeme ilişkisini kaydet
                    insertPaymentLog(paymentId, customerId, paymentAmount, invoiceId);
                    insertPaymentInvoiceRelation(paymentId, invoiceId);

                    System.out.println("Fatura " + invoiceId + " için kısmi ödeme yapıldı: " + paymentAmount);

                    paymentAmount = 0;
                }
            }

            // Eğer hala ödeme miktarı kaldıysa, fazla ödeme olarak kaydet
            if (paymentAmount > 0) {
                customer.addExtraPayment(paymentAmount);
                updateExcessPayment(customerId, customer.getExtraPayment());
                System.out.println("Kalan miktar (" + paymentAmount + ") müşterinin fazla ödemesi olarak kaydedildi.");
            }

        } catch (SQLException e) {
            System.out.println("Ödeme yapılırken hata oluştu: " + e.getMessage());
        }

        return paymentAmount;
    }

    // Ödeme logunu ekle
    public static void insertPaymentLog(String paymentId, String customerId, double paidAmount, int invoiceId) {
        String insertPaymentLogSQL = "INSERT INTO PaymentLog (paymentId, customerId, paidAmount, paymentDate, invoiceId) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertPaymentLogSQL)) {
            pstmt.setString(1, paymentId);
            pstmt.setString(2, customerId);
            pstmt.setDouble(3, paidAmount);
            pstmt.setString(4, LocalDate.now().toString());
            pstmt.setInt(5, invoiceId); // invoiceId'yi buraya ekledik
            pstmt.executeUpdate();
            System.out.println("Ödeme logu başarıyla kaydedildi, Fatura ID: " + invoiceId);
        } catch (SQLException e) {
            System.out.println("Ödeme logu kaydedilirken hata oluştu: " + e.getMessage());
        }
    }

    // Ödeme-Fatura ilişkisini eklerken tekrarları önle
    public static void insertPaymentInvoiceRelation(String paymentId, int invoiceId) {
        String checkExistenceSQL = "SELECT COUNT(*) AS count FROM PaymentInvoiceRelation WHERE paymentId = ? AND invoiceId = ?";

        try (PreparedStatement pstmtCheck = connection.prepareStatement(checkExistenceSQL)) {
            pstmtCheck.setString(1, paymentId);
            pstmtCheck.setInt(2, invoiceId);
            ResultSet rs = pstmtCheck.executeQuery();

            if (rs.next() && rs.getInt("count") == 0) {
                // Eğer ilişki yoksa, ekleyelim
                String insertPaymentInvoiceRelationSQL = "INSERT INTO PaymentInvoiceRelation (paymentId, invoiceId) VALUES (?, ?)";

                try (PreparedStatement pstmtInsert = connection.prepareStatement(insertPaymentInvoiceRelationSQL)) {
                    pstmtInsert.setString(1, paymentId);
                    pstmtInsert.setInt(2, invoiceId);
                    pstmtInsert.executeUpdate();
                    System.out.println("Ödeme ve fatura ilişkisi başarıyla kaydedildi, Fatura ID: " + invoiceId);
                }
            } else {
                System.out.println("Bu ödeme-fatura ilişkisi zaten var, tekrar eklenmedi.");
            }
        } catch (SQLException e) {
            System.out.println("Ödeme ve fatura ilişkisi kontrol edilirken veya eklenirken hata oluştu: " + e.getMessage());
        }
    }

    public static void viewAllPaymentLogs() {
        String query = "SELECT * FROM PaymentLog";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String paymentId = rs.getString("paymentId");
                int invoiceId = rs.getInt("invoiceId");
                String customerId = rs.getString("customerId");
                double paidAmount = rs.getDouble("paidAmount");
                String paymentDate = rs.getString("paymentDate");

                System.out.println("Ödeme ID: " + paymentId +
                        ", Fatura ID: " + invoiceId +
                        ", Müşteri ID: " + customerId +
                        ", Ödenen Tutar: " + paidAmount +
                        ", Ödeme Tarihi: " + paymentDate);
            }
        } catch (SQLException e) {
            System.out.println("Ödeme logları görüntülenirken hata oluştu: " + e.getMessage());
        }
    }


    public static void displayCustomerPaymentSummary(String customerId) {
        String query = "SELECT " +
                "pl.paymentId, " +
                "pl.paymentDate, " +
                "c.id AS customerId, " +
                "c.firstName || ' ' || c.lastName AS customerName, " +
                "c.excessPayment, " +
                "pl.invoiceId, " +
                "SUM(pl.paidAmount) AS totalPaid " +
                "FROM PaymentLog pl " +
                "JOIN Customer c ON pl.customerId = c.id " +
                "WHERE pl.customerId = ? " +
                "GROUP BY pl.paymentId, pl.invoiceId " +
                "ORDER BY pl.paymentDate ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            Map<String, List<PaymentDetail>> paymentSummary = new LinkedHashMap<>();
            double totalExcessPayment = 0.0;
            String customerName = "";

            while (rs.next()) {
                String paymentId = rs.getString("paymentId");
                String paymentDate = rs.getString("paymentDate");
                int invoiceId = rs.getInt("invoiceId");
                double totalPaid = rs.getDouble("totalPaid");
                totalExcessPayment = rs.getDouble("excessPayment");
                customerName = rs.getString("customerName");

                PaymentDetail detail = new PaymentDetail(invoiceId, totalPaid);

                paymentSummary
                        .computeIfAbsent(paymentId, k -> new ArrayList<>())
                        .add(detail);
            }

            if (paymentSummary.isEmpty()) {
                System.out.println("Bu müşteri için ödeme kaydı bulunamadı.");
                return;
            }

            System.out.println("Müşteri Adı: " + customerName);
            System.out.println("Müşteri ID: " + customerId);
            System.out.println("Müşterinin Şirkette Kalan Fazla Ödemesi: " + totalExcessPayment);

            for (Map.Entry<String, List<PaymentDetail>> entry : paymentSummary.entrySet()) {
                String paymentId = entry.getKey();
                List<PaymentDetail> details = entry.getValue();

                System.out.println("\nÖdeme ID: " + paymentId);

                double paymentTotal = 0.0;
                for (PaymentDetail detail : details) {
                    System.out.println("- Fatura ID: " + detail.invoiceId + ", Ödenen Tutar: " + detail.paidAmount);
                    paymentTotal += detail.paidAmount;
                }

                System.out.println("Bu Ödeme için Toplam Ödenen Tutar: " + paymentTotal);
            }

        } catch (SQLException e) {
            System.out.println("Müşteri ödeme özeti görüntülenirken hata oluştu: " + e.getMessage());
        }
    }

    static class PaymentDetail {
        int invoiceId;
        double paidAmount;

        public PaymentDetail(int invoiceId, double paidAmount) {
            this.invoiceId = invoiceId;
            this.paidAmount = paidAmount;
        }
    }


    public static Customer getCustomerById(String customerId) {
        String query = "SELECT * FROM Customer WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Customer(
                        rs.getString("id"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getBoolean("isGroupCompany")
                );
            }

        } catch (SQLException e) {
            System.out.println("Müşteri bilgisi alınırken hata oluştu: " + e.getMessage());
        }
        return null;
    }

    // Faturayı ödenmiş olarak güncelle
    private static void updateInvoiceAsPaid(int invoiceId) {
        String updateInvoiceSQL = "UPDATE Invoice SET isPaid = 1, totalDebt = 0, paymentDate = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateInvoiceSQL)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setInt(2, invoiceId);
            pstmt.executeUpdate();
            System.out.println("Fatura başarıyla ödendi.");

        } catch (SQLException e) {
            System.out.println("Fatura güncellenirken hata oluştu: " + e.getMessage());
        }
    }


    // Fatura tutarını güncellemek yerine totalDebt üzerinden güncelleme yapıldı
    private static void updateInvoiceAmount(int invoiceId, double newTotalDebt) {
        String updateAmountSQL = "UPDATE Invoice SET totalDebt = ?, isPaid = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateAmountSQL)) {
            pstmt.setDouble(1, newTotalDebt);
            pstmt.setInt(2, newTotalDebt == 0 ? 1 : 0); // Eğer borç kalmamışsa isPaid = 1 yap
            pstmt.setInt(3, invoiceId);
            pstmt.executeUpdate();
            System.out.println("Fatura toplam borcu güncellendi.");
        } catch (SQLException e) {
            System.out.println("Fatura toplam borcu güncellenirken hata oluştu: " + e.getMessage());
        }
    }


    // Müşteri borcunu görüntüleme
    public static double getCustomerDebt(String customerId) {
        String query = "SELECT SUM(totalDebt) AS totalDebt FROM Invoice WHERE customerId = ? AND isPaid = 0";
        double totalDebt = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                totalDebt = rs.getDouble("totalDebt");
            }

        } catch (SQLException e) {
            System.out.println("Müşteri borcu görüntülenirken hata oluştu: " + e.getMessage());
        }

        return totalDebt;
    }


    // Vadeye kalan günleri hesaplama
    public static void getDaysUntilDue(String customerId) {
        String query = "SELECT id, dueDate, isPaid FROM Invoice WHERE customerId = ? ORDER BY dueDate ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int invoiceId = rs.getInt("id");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                boolean isPaid = rs.getInt("isPaid") == 1;
                long daysUntilDue = LocalDate.now().until(dueDate).getDays();

                System.out.println("Fatura ID: " + invoiceId + ", Vadeye Kalan Günler: " + daysUntilDue + ", Ödenmiş: " + (isPaid ? "Evet" : "Hayır"));
            }

        } catch (SQLException e) {
            System.out.println("Vadeye kalan günler görüntülenirken hata oluştu: " + e.getMessage());
        }
    }


    // Gecikme faizi hesaplama

    public static double getLateFee(String customerId) {
        String query = "SELECT i.id, i.amount, i.dueDate, c.isGroupCompany, i.isPaid " +
                "FROM Invoice i " +
                "INNER JOIN Customer c ON i.customerId = c.id " +
                "WHERE i.customerId = ? AND i.isPaid = 0 AND i.dueDate < ?";

        double totalLateFee = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            pstmt.setString(2, LocalDate.now().toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int invoiceId = rs.getInt("id");
                double amount = rs.getDouble("amount");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                boolean isGroupCompany = rs.getBoolean("isGroupCompany");
                boolean isPaid = rs.getBoolean("isPaid");

                long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
                double INTEREST_RATE = InterestRateCalculator.InterestCalculation(isGroupCompany, overdueDays, false);

                double lateFee = overdueDays * INTEREST_RATE * amount;

                // Faiz borcunu ve toplam borcu güncelle
                updateInvoiceLateFee(invoiceId, lateFee);

                totalLateFee += lateFee;

                System.out.println("Fatura ID: " + invoiceId + ", Gecikme Faizi: " + lateFee + ", Ödenmiş: " + isPaid);
            }

        } catch (SQLException e) {
            System.out.println("Gecikme faizi hesaplanırken hata oluştu: " + e.getMessage());
        }

        return totalLateFee;
    }

    public static double getInvoiceLateFee(int invoiceId) {
        String query = "SELECT i.amount, i.dueDate, c.isGroupCompany, i.isPaid " +
                "FROM Invoice i " +
                "INNER JOIN Customer c ON i.customerId = c.id " +
                "WHERE i.id = ?";

        double lateFee = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, invoiceId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double amount = rs.getDouble("amount");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                boolean isGroupCompany = rs.getBoolean("isGroupCompany");
                boolean isPaid = rs.getBoolean("isPaid");

                long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
                double INTEREST_RATE = InterestRateCalculator.InterestCalculation(isGroupCompany, overdueDays, false);

                lateFee = overdueDays * INTEREST_RATE * amount;

                // Faiz borcunu ve toplam borcu güncelle
                updateInvoiceLateFee(invoiceId, lateFee);

                System.out.println("Fatura ID: " + invoiceId + ", Gecikme Faizi: " + lateFee + ", Ödenmiş: " + isPaid);
            }

        } catch (SQLException e) {
            System.out.println("Gecikme faizi hesaplanırken hata oluştu: " + e.getMessage());
        }

        return lateFee;
    }


    // Gecikme faizi ve toplam borcu güncelleyen metot
    private static void updateInvoiceLateFee(int invoiceId, double lateFee) {
        String updateLateFeeSQL = "UPDATE Invoice SET lateFee = ?, totalDebt = amount + ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateLateFeeSQL)) {
            pstmt.setDouble(1, lateFee);
            pstmt.setDouble(2, lateFee);
            pstmt.setInt(3, invoiceId);
            pstmt.executeUpdate();
            System.out.println("Gecikme faizi ve toplam borç güncellendi.");

        } catch (SQLException e) {
            System.out.println("Gecikme faizi güncellenirken hata oluştu: " + e.getMessage());
        }
    }

    // Tüm faturaları görüntüleme
    public static void viewAllInvoices() {
        String query = "SELECT Invoice.id, Customer.firstName, Customer.lastName, Customer.isGroupCompany, " +
                "Invoice.amount, Invoice.lateFee, Invoice.totalDebt, Invoice.dueDate, " +
                "Invoice.paymentDate, Invoice.isPaid " +
                "FROM Invoice INNER JOIN Customer ON Invoice.customerId = Customer.id " +
                "ORDER BY Invoice.dueDate ASC";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Müşteri bilgilerini çekiyoruz
                int invoiceId = rs.getInt("id");
                String customerName = rs.getString("firstName") + " " + rs.getString("lastName");
                boolean isGroupCompany = rs.getBoolean("isGroupCompany"); // Grup şirketi bilgisi
                double amount = rs.getDouble("amount");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                LocalDate paymentDate = rs.getString("paymentDate") != null ? LocalDate.parse(rs.getString("paymentDate")) : null;
                boolean isPaid = rs.getInt("isPaid") == 1;
                double lateFee = rs.getDouble("lateFee");
                double totalDebt = rs.getDouble("totalDebt");

                if (isPaid) {
                    // Eğer ödeme yapıldıysa gecikme faizi ve toplam borcu güncelleme
                    System.out.println("Müşteri: " + customerName + ", Ana Para: " + amount +
                            ", Gecikme Faizi: " + lateFee + ", Toplam Borç: " + totalDebt +
                            ", Vade Tarihi: " + dueDate + ", Ödenmiş: " + isPaid);
                    continue;
                }

                // Gecikme Faizi ve Toplam Borcu yeniden hesapla
                long overdueDays = 0;
                boolean isEarlyPayment = false;

                if (!isPaid && LocalDate.now().isAfter(dueDate)) {
                    overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
                }

                // Faiz oranını hesapla
                final double INTEREST_RATE = InterestRateCalculator.InterestCalculation(isGroupCompany, overdueDays, isEarlyPayment);

                if (!isPaid && LocalDate.now().isAfter(dueDate)) {
                    lateFee = overdueDays * INTEREST_RATE * amount;
                    totalDebt = amount + lateFee;

                    // Veritabanında gecikme faizini güncelle
                    updateInvoiceLateFee(invoiceId, lateFee);
                }

                System.out.println("Müşteri: " + customerName + ", Ana Para: " + amount +
                        ", Gecikme Faizi: " + lateFee + ", Toplam Borç: " + totalDebt +
                        ", Vade Tarihi: " + dueDate + ", Ödenmiş: " +
                        isPaid);
            }

        } catch (SQLException e) {
            System.out.println("Faturalar görüntülenirken hata oluştu: " + e.getMessage());
        }
    }

    public static String getInvoiceDebt(int invoiceId) {
        String query = "SELECT totalDebt, isPaid FROM Invoice WHERE id = ?";
        double invoiceDebt = 0;
        boolean isPaid = false;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, invoiceId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                invoiceDebt = rs.getDouble("totalDebt");
                isPaid = rs.getInt("isPaid") == 1;
            }
        } catch (SQLException e) {
            System.out.println("Fatura borcu görüntülenirken hata oluştu: " + e.getMessage());
        }

        return "Fatura Borcu: " + invoiceDebt + ", Ödenmiş: " + (isPaid ? "Evet" : "Hayır");
    }

    public static void updateLateFees() {
        String query = "SELECT i.id, i.amount, i.dueDate, c.isGroupCompany, i.isPaid " +
                "FROM Invoice i " +
                "INNER JOIN Customer c ON i.customerId = c.id " +
                "WHERE i.isPaid = 0 AND i.dueDate < ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, LocalDate.now().toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int invoiceId = rs.getInt("id");
                double amount = rs.getDouble("amount");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                boolean isGroupCompany = rs.getBoolean("isGroupCompany");

                long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
                double INTEREST_RATE = InterestRateCalculator.InterestCalculation(isGroupCompany, overdueDays, false);
                double lateFee = overdueDays * INTEREST_RATE * amount;

                // Gecikme faizi ve toplam borcu güncelle
                updateInvoiceLateFee(invoiceId, lateFee);
            }

        } catch (SQLException e) {
            System.out.println("Gecikme faizi hesaplanırken hata oluştu: " + e.getMessage());
        }
    }

    public static void applyExcessPaymentToInvoice(String customerId, int invoiceId, double excessPayment) {
        // Öncelikle, seçilen faturayı getir
        String query = "SELECT totalDebt FROM Invoice WHERE id = ? AND customerId = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, invoiceId);
            pstmt.setString(2, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double totalDebt = rs.getDouble("totalDebt");

                String paymentId = UUID.randomUUID().toString(); // Her işlem için benzersiz bir ödeme ID'si oluştur

                if (excessPayment >= totalDebt) {
                    // Fazla ödeme faturayı tamamen karşılıyorsa
                    updateInvoiceAsPaid(invoiceId); // Faturayı ödenmiş olarak işaretle
                    updateExcessPayment(customerId, excessPayment - totalDebt); // Kalan fazla ödemeyi güncelle

                    // Ödeme logunu kaydet
                    insertPaymentLog(paymentId, customerId, totalDebt, invoiceId);
                    insertPaymentInvoiceRelation(paymentId, invoiceId);

                    System.out.println("Fatura fazla ödeme ile ödendi, kalan fazla ödeme: " + (excessPayment - totalDebt));
                } else {
                    // Fazla ödeme faturanın bir kısmını karşılıyorsa
                    updateInvoiceAmount(invoiceId, totalDebt - excessPayment); // Fatura tutarını azalt
                    updateExcessPayment(customerId, 0); // Fazla ödeme sıfırlanır

                    // Ödeme logunu kaydet
                    insertPaymentLog(paymentId, customerId, excessPayment, invoiceId);
                    insertPaymentInvoiceRelation(paymentId, invoiceId);

                    System.out.println("Fazla ödeme faturaya uygulandı, kalan borç: " + (totalDebt - excessPayment));
                }
            }
        } catch (SQLException e) {
            System.out.println("Fatura için fazla ödeme uygulanırken hata oluştu: " + e.getMessage());
        }
    }



    public static List<Invoice> getUnpaidInvoicesForCustomer(String customerId) {
        List<Invoice> unpaidInvoices = new ArrayList<>();
        String query = "SELECT id, amount, dueDate FROM Invoice WHERE customerId = ? AND isPaid = 0 ORDER BY dueDate ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int invoiceId = rs.getInt("id");
                double amount = rs.getDouble("amount");
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));

                // Invoice nesnesini oluştururken ID'yi de çekiyoruz
                Invoice invoice = new Invoice(amount, dueDate);
                invoice.setId(invoiceId);  // ID burada set ediliyor
                unpaidInvoices.add(invoice);
            }

        } catch (SQLException e) {
            System.out.println("Ödenmemiş faturalar alınırken hata oluştu: " + e.getMessage());
        }

        return unpaidInvoices;
    }
}
