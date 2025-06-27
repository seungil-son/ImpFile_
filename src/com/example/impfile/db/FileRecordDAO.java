package com.example.impfile.db;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite를 이용해 FileRecord를 CRUD 및 조회하는 DAO
 * 삭제 시에는 DB 레코드를 남겨두고, 백업 디렉토리만 재귀 삭제합니다.
 * 이제 백업 정리 시 DB 레코드도 함께 삭제할 수 있습니다.
 */
public class FileRecordDAO {
    private static final String DB_URL = "jdbc:sqlite:data.db";

    public FileRecordDAO() {
        // 테이블이 없으면 생성
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement  stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS FileRecord (" +
                            " groupName    TEXT PRIMARY KEY," +
                            " filePath     TEXT NOT NULL," +
                            " score        INTEGER NOT NULL," +
                            " lastModified INTEGER NOT NULL" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 그룹명으로 레코드 조회 */
    public FileRecord findByGroup(String group) {
        String sql = "SELECT groupName, filePath, score, lastModified " +
                "FROM FileRecord WHERE groupName=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FileRecord(
                            rs.getString("groupName"),
                            rs.getString("filePath"),
                            rs.getInt("score"),
                            rs.getLong("lastModified")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 전체 레코드 조회 */
    public List<FileRecord> findAll() {
        List<FileRecord> list = new ArrayList<>();
        String sql = "SELECT groupName, filePath, score, lastModified FROM FileRecord";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new FileRecord(
                        rs.getString("groupName"),
                        rs.getString("filePath"),
                        rs.getInt("score"),
                        rs.getLong("lastModified")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** INSERT OR REPLACE */
    public void insertOrReplace(FileRecord rec) {
        String sql = "INSERT OR REPLACE INTO FileRecord " +
                "(groupName, filePath, score, lastModified) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rec.getGroupName());
            ps.setString(2, rec.getFilePath());
            ps.setInt(3, rec.getScore());
            ps.setLong(4, rec.getLastModified());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 물리 파일이 없는 경우 백업 디렉토리만 삭제.
     * DB 레코드는 남겨두어 복구 가능하도록 유지합니다.
     */
    public void removeOrphanBackups() {
        String sql = "SELECT groupName, filePath FROM FileRecord";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String grp  = rs.getString("groupName");
                String path = rs.getString("filePath");
                if (!new File(path).exists()) {
                    File backupDir = new File("backup", grp);
                    deleteDirectoryRecursively(backupDir);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** DB 전체 레코드 삭제 */
    public void deleteAll() {
        String sql = "DELETE FROM FileRecord";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 그룹명으로 레코드 삭제 */
    public void deleteByGroup(String groupName) {
        String sql = "DELETE FROM FileRecord WHERE groupName=?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** 디렉토리를 재귀 삭제 */
    private void deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectoryRecursively(f);
                else                 f.delete();
            }
        }
        dir.delete();
    }
}
