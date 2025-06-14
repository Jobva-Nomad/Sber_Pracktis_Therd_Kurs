package org.example;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Properties props = new Properties();

        try(InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if(input == null) {
                System.out.println("Не найден файл конфига");
                return;
            }

            props.load(input);
            String url = props.getProperty("jdbc.url");
            String username = props.getProperty("jdbc.username");
            String password = props.getProperty("jdbc.password");

            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Scanner scanner = new Scanner(System.in)) {

                System.out.println("Подключение установлено, введите SQL выражение");

                while (true){
                    System.out.print("> ");
                    String sql = scanner.nextLine().trim();

                    if (sql.equalsIgnoreCase("QUIT")){
                        System.out.println("Завершение работы....");
                        break;
                    }

                    try (Statement stmt = conn.createStatement()) {
                        boolean hasResultSet = stmt.execute(sql);

                        if (hasResultSet) {
                            if (sql.toLowerCase().startsWith("select")) {
                                processSelect(conn, sql);
                            } else {
                                try (ResultSet rs = stmt.getResultSet()) {
                                    System.out.println("Результат: ");
                                    while (rs.next()) {
                                        System.out.println(rs.getString(1));
                                    }
                                }
                            }
                        } else {
                            int updateCount = stmt.getUpdateCount();
                            System.out.println("Успешно, изменено строк: " + updateCount);
                        }
                    } catch (SQLException ex) {
                        System.out.println("Ошибка при выполнении SQL запроса: ");
                        System.out.println(ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка подключения к БД:");
            e.printStackTrace();
        }
    }

    private static void processSelect(Connection conn, String origSql) throws SQLException {
        String limitedSql = origSql.toLowerCase().contains(" limit ") ? origSql : origSql + " LIMIT 10";
        String countSql = "SELECT COUNT(*) FROM (" + origSql + ") AS total_count";

        try (
                Statement countStmt = conn.createStatement();
                ResultSet countRs = countStmt.executeQuery(countSql);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(limitedSql)
        ) {
            countRs.next();
            int total = countRs.getInt(1);

            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            int rowCount = 0;

            System.out.println("Результат запроса:");

            for (int i = 1; i <= columns; i++) {
                System.out.print(meta.getColumnName(i) + "\t");
            }
            System.out.println();

            while (rs.next()) {
                for (int i = 1; i <= columns; i++){
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
                rowCount++;
            }
            if (total > 10) {
                System.out.println("В БД есть еще записи (" + (total - 10) + " не показано)");
            }
        }
    }
}