package com.example.fifoproject;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

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
    // Fatura ekleme
    public static void insertInvoice(String customerId, double amount, String dueDate) {
        String insertInvoiceSQL = "INSERT INTO Invoice (customerId, amount, dueDate, totalDebt, isPaid) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertInvoiceSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customerId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, dueDate);

            // Müşterinin fazla ödemesi var mı kontrol et
            double excessPayment = getExcessPayment(customerId);
            boolean invoicePaid = false;
            double totalDebt = amount;

            // Eğer fazla ödeme varsa yeni faturaya uygula
            if (excessPayment > 0) {
                if (excessPayment >= amount) {
                    // Fazla ödeme faturayı tamamen karşılıyorsa
                    invoicePaid = true;
                    updateExcessPayment(customerId, excessPayment - amount); // Kalan fazla ödemeyi güncelle
                    System.out.println("Fatura fazla ödeme ile ödendi, kalan fazla ödeme: " + (excessPayment - amount));
                    totalDebt = 0;
                } else {
                    // Fazla ödeme faturanın bir kısmını karşılıyorsa
                    updateExcessPayment(customerId, 0); // Fazla ödeme sıfırlanıyor
                    totalDebt = amount - excessPayment; // Borç, fazla ödeme kadar azaltılıyor
                    System.out.println("Fazla ödeme faturaya uygulandı, kalan borç: " + totalDebt);
                }
            }

            // Fatura ödendi mi, edilmedi mi ona göre kaydediyoruz
            pstmt.setDouble(4, totalDebt);
            pstmt.setInt(5, invoicePaid ? 1 : 0); // Eğer fatura ödendiyse isPaid = 1

            pstmt.executeUpdate();

            // Fatura ID'sini almak için
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
                    updateInvoiceAsPaid(invoiceId);

                    // Fatura ve ödeme ilişkisini doğru bir şekilde kaydet
                    insertPaymentLog(paymentId, customerId, invoiceAmount, invoiceId);
                    insertPaymentInvoiceRelation(paymentId, invoiceId);

                    System.out.println("Fatura " + invoiceId + " tamamen ödendi.");
                } else if (paymentAmount > 0 && paymentAmount < invoiceAmount) {
                    // Kısmi ödeme
                    double remainingDebt = invoiceAmount - paymentAmount;
                    updateInvoiceAmount(invoiceId, remainingDebt);

                    // Fatura ve ödeme ilişkisini doğru bir şekilde kaydet
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
        // İlk önce bu ödeme-fatura ilişkisinin zaten var olup olmadığını kontrol edelim
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


    public static void viewPaymentInvoiceRelations() {
        String query = "SELECT pir.paymentId, pir.invoiceId, pl.customerId, pl.paidAmount, pl.paymentDate " +
                "FROM PaymentInvoiceRelation pir " +
                "JOIN PaymentLog pl ON pir.paymentId = pl.paymentId " +
                "ORDER BY pir.paymentId";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

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
            System.out.println("Fatura-Ödeme ilişkileri görüntülenirken hata oluştu: " + e.getMessage());
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
        String updateInvoiceSQL = "UPDATE Invoice SET isPaid = 1, paymentDate = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateInvoiceSQL)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setInt(2, invoiceId);
            pstmt.executeUpdate();
            System.out.println("Fatura başarıyla ödendi.");

        } catch (SQLException e) {
            System.out.println("Fatura güncellenirken hata oluştu: " + e.getMessage());
        }
    }

    // Fatura tutarını güncelle
    private static void updateInvoiceAmount(int invoiceId, double newAmount) {
        String updateAmountSQL = "UPDATE Invoice SET amount = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateAmountSQL)) {
            pstmt.setDouble(1, newAmount);
            pstmt.setInt(2, invoiceId);
            pstmt.executeUpdate();
            System.out.println("Fatura tutarı güncellendi.");

        } catch (SQLException e) {
            System.out.println("Fatura tutarı güncellenirken hata oluştu: " + e.getMessage());
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
    public static int getDaysUntilDue(String customerId) {
        String query = "SELECT dueDate FROM Invoice WHERE customerId = ? AND isPaid = 0 ORDER BY dueDate ASC LIMIT 1";
        int daysUntilDue = -1;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                LocalDate dueDate = LocalDate.parse(rs.getString("dueDate"));
                daysUntilDue = (int) LocalDate.now().until(dueDate).getDays();
            }

        } catch (SQLException e) {
            System.out.println("Vadeye kalan günler hesaplanırken hata oluştu: " + e.getMessage());
        }

        return daysUntilDue;
    }

    // Gecikme faizi hesaplama
    public static double getLateFee(String customerId) throws SQLException {
        String query = "SELECT * FROM Invoice WHERE customerId = ? AND isPaid = 0 AND dueDate < ?";
        String query2 = "SELECT \n" +
                "    Invoice.id, \n" +
                "    Invoice.amount, \n" +
                "    Invoice.dueDate, \n" +
                "    Invoice.paymentDate, \n" +
                "    Invoice.isPaid, \n" +
                "    Customer.firstName, \n" +
                "    Customer.lastName, \n" +
                "    Customer.isGroupCompany \n" +
                "FROM \n" +
                "    Invoice \n" +
                "INNER JOIN \n" +
                "    Customer \n" +
                "ON \n" +
                "    Invoice.customerId = Customer.id \n" +
                "WHERE \n" +
                "    Invoice.customerId = ? \n" +
                "AND \n" +
                "    Invoice.isPaid = 0 \n" +
                "AND \n" +
                "    Invoice.dueDate < ?\n";

        double totalLateFee = 0;
        PreparedStatement pstmt1 = connection.prepareStatement(query2);
        ResultSet rs2= pstmt1.executeQuery();
        boolean isGroupCompany = rs2.getBoolean("isGroupCompany");
        LocalDate dueDate = LocalDate.parse(rs2.getString("dueDate"));
        long overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());

        boolean isEarlyPayment = rs2.getBoolean("isEarlyPayment");

        double INTEREST_RATE = InterestRateCalculator.InterestCalculation(isGroupCompany, overdueDays, isEarlyPayment); // %2 gecikme faizi

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, customerId);
            pstmt.setString(2, LocalDate.now().toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                dueDate = LocalDate.parse(rs.getString("dueDate"));
                overdueDays = Math.abs(LocalDate.now().until(dueDate).getDays());
                double amount = rs.getDouble("amount");
                double lateFee = overdueDays * INTEREST_RATE * amount;

                // Faiz borcunu tutacak sütunu güncelle
                int invoiceId = rs.getInt("id");
                updateInvoiceLateFee(invoiceId, lateFee);

                totalLateFee += lateFee;
            }

        } catch (SQLException e) {
            System.out.println("Gecikme faizi hesaplanırken hata oluştu: " + e.getMessage());
        }

        return totalLateFee;
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



    // Toplu ödeme yapma
    public static void makeBulkPayment(String customerId, double totalPayment) {
        String selectUnpaidInvoicesSQL = "SELECT * FROM Invoice WHERE customerId = ? AND isPaid = 0 ORDER BY dueDate ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(selectUnpaidInvoicesSQL)) {
            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next() && totalPayment > 0) {
                double invoiceAmount = rs.getDouble("amount");
                int invoiceId = rs.getInt("id");

                if (totalPayment >= invoiceAmount) {
                    // Fatura tamamen ödeniyor
                    totalPayment -= invoiceAmount;
                    updateInvoiceAsPaid(invoiceId);
                } else {
                    // Fatura kısmen ödeniyor
                    updateInvoiceAmount(invoiceId, invoiceAmount - totalPayment);
                    totalPayment = 0;
                }
            }

            System.out.println("Toplu ödeme yapıldı.");

        } catch (SQLException e) {
            System.out.println("Toplu ödeme yapılırken hata oluştu: " + e.getMessage());
        }
    }

    // Sıradaki müşteriyi işleme
    public static void processNextCustomer() {
        // Sıradaki müşteriyi işleme mantığını buraya ekleyebilirsin.
        // Örneğin, ödeme yapma, borç gösterme gibi işlemler yapılabilir.
        System.out.println("Sıradaki müşteri işleniyor...");
    }

    // Tüm faturaları görüntüleme
    public static void viewAllInvoices() {
        String query = "SELECT Invoice.id, Customer.firstName, Customer.lastName, Customer.isGroupCompany, Invoice.amount, Invoice.lateFee, Invoice.totalDebt, Invoice.dueDate, Invoice.paymentDate, Invoice.isPaid " +
                "FROM Invoice INNER JOIN Customer ON Invoice.customerId = Customer.id ORDER BY Invoice.dueDate ASC";

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

                // Gecikme Faizi ve Toplam Borcu yeniden hesapla
                long overdueDays = 0;
                boolean isEarlyPayment = false;

                if (isPaid && paymentDate != null && paymentDate.isBefore(dueDate)) {
                    isEarlyPayment = true;
                } else if (!isPaid && LocalDate.now().isAfter(dueDate)) {
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
                        ", Vade Tarihi: " + dueDate + ", Ödenmiş: " + isPaid);
            }

        } catch (SQLException e) {
            System.out.println("Faturalar görüntülenirken hata oluştu: " + e.getMessage());
        }
    }

}