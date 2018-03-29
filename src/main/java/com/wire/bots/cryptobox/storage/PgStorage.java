package com.wire.bots.cryptobox.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;

import java.io.ByteArrayInputStream;
import java.sql.*;

public class PgStorage implements IStorage {

    @Override
    public IRecord fetch(String id, String sid) {
        String key = key(id, sid);
        try {
            Connection c = newConnection();
            PreparedStatement stmt = c.prepareStatement("SELECT data FROM sessions WHERE id = ? FOR UPDATE");
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            byte[] data = null;
            if (rs.next()) {
                data = rs.getBytes("data");
            }
            return new Record(data, c, key);
        } catch (Exception e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    private Connection newConnection() throws SQLException, InterruptedException {
        while (true) {
            try {
                Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres");
                connection.setAutoCommit(false);
                return connection;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
    }

    private String key(String id, String sid) {
        return id + sid;
    }

    class Record implements IRecord {
        private final byte[] data;
        private final Connection connection;
        private final String sid;

        Record(byte[] data, Connection connection, String sid) {
            this.data = data;
            this.connection = connection;
            this.sid = sid;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public void persist(byte[] data) {
            try {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO sessions (id, data) VALUES (?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET data = ?");
                stmt.setString(1, sid);
                stmt.setBinaryStream(2, new ByteArrayInputStream(data));
                stmt.setBinaryStream(3, new ByteArrayInputStream(data));

                stmt.executeUpdate();

                stmt.close();
            } catch (Exception e) {
                System.out.println(e.getClass().getName() + ": " + e.getMessage());
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            } finally {
                try {
                    connection.commit();
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
