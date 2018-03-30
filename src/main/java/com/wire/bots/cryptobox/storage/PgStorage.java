package com.wire.bots.cryptobox.storage;

import com.wire.bots.cryptobox.IRecord;
import com.wire.bots.cryptobox.IStorage;

import java.io.ByteArrayInputStream;
import java.sql.*;

public class PgStorage implements IStorage {
    private final String user;
    private final String password;
    private final String db;
    private final String host;
    private final int port;

    public PgStorage(String user, String password, String db, String host, int port) {
        this.user = user;
        this.password = password;
        this.db = db;
        this.host = host;
        this.port = port;
    }

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
                String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
                Connection connection = DriverManager.getConnection(url, user, password);
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
            String sql = "INSERT INTO sessions (id, data) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, sid);
                stmt.setBinaryStream(2, new ByteArrayInputStream(data));
                stmt.executeUpdate();
            } catch (Exception e) {
                System.out.println(e.getClass().getName() + ": " + e.getMessage());
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
