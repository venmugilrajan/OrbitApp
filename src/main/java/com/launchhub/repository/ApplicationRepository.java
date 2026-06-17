package com.launchhub.repository;

import com.launchhub.model.ApplicationInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationRepository {

    public List<ApplicationInfo> getAllApplications() {
        List<ApplicationInfo> apps = new ArrayList<>();
        String sql = "SELECT * FROM applications ORDER BY name COLLATE NOCASE ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                apps.add(mapResultSetToApp(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }

    public List<ApplicationInfo> getFavorites() {
        List<ApplicationInfo> apps = new ArrayList<>();
        String sql = "SELECT * FROM applications WHERE is_favorite = 1 ORDER BY name COLLATE NOCASE ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                apps.add(mapResultSetToApp(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }

    public List<ApplicationInfo> getRecentApplications(int limit) {
        List<ApplicationInfo> apps = new ArrayList<>();
        String sql = """
            SELECT a.* FROM applications a
            JOIN usage_history h ON a.id = h.app_id
            GROUP BY a.id
            ORDER BY MAX(h.launched_at) DESC
            LIMIT ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    apps.add(mapResultSetToApp(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }

    public void save(ApplicationInfo app) {
        String sql = """
            INSERT OR REPLACE INTO applications 
            (name, publisher, version, install_location, install_date, size_bytes, executable_path, icon_path, category, is_favorite, launch_count, last_used)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, app.getName());
            pstmt.setString(2, app.getPublisher());
            pstmt.setString(3, app.getVersion());
            pstmt.setString(4, app.getInstallLocation());
            pstmt.setString(5, app.getInstallDate());
            pstmt.setLong(6, app.getSizeBytes());
            pstmt.setString(7, app.getExecutablePath());
            pstmt.setString(8, app.getIconPath());
            pstmt.setString(9, app.getCategory());
            pstmt.setInt(10, app.isFavorite() ? 1 : 0);
            pstmt.setInt(11, app.getLaunchCount());
            pstmt.setString(12, app.getLastUsed());

            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    app.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(ApplicationInfo app) {
        String sql = """
            UPDATE applications SET
            name = ?, publisher = ?, version = ?, install_location = ?, install_date = ?, size_bytes = ?, icon_path = ?, category = ?, is_favorite = ?, launch_count = ?, last_used = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, app.getName());
            pstmt.setString(2, app.getPublisher());
            pstmt.setString(3, app.getVersion());
            pstmt.setString(4, app.getInstallLocation());
            pstmt.setString(5, app.getInstallDate());
            pstmt.setLong(6, app.getSizeBytes());
            pstmt.setString(7, app.getIconPath());
            pstmt.setString(8, app.getCategory());
            pstmt.setInt(9, app.isFavorite() ? 1 : 0);
            pstmt.setInt(10, app.getLaunchCount());
            pstmt.setString(11, app.getLastUsed());
            pstmt.setInt(12, app.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setFavorite(int appId, boolean isFavorite) {
        String sql = "UPDATE applications SET is_favorite = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, isFavorite ? 1 : 0);
            pstmt.setInt(2, appId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordLaunch(int appId, String timestamp) {
        String updateApp = "UPDATE applications SET launch_count = launch_count + 1, last_used = ? WHERE id = ?";
        String insertHistory = "INSERT INTO usage_history (app_id, launched_at) VALUES (?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(updateApp);
                 PreparedStatement p2 = conn.prepareStatement(insertHistory)) {
                
                p1.setString(1, timestamp);
                p1.setInt(2, appId);
                p1.executeUpdate();

                p2.setInt(1, appId);
                p2.setString(2, timestamp);
                p2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT category, COUNT(*) as cnt FROM applications GROUP BY category";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String cat = rs.getString("category");
                if (cat == null || cat.trim().isEmpty()) {
                    cat = "Others";
                }
                counts.put(cat, rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    public List<ApplicationInfo> getUnusedApplications(int days) {
        List<ApplicationInfo> apps = new ArrayList<>();
        // Query to check if last_used is empty/null OR if last_used is older than X days.
        // SQLite datetime function: datetime('now', '-30 days')
        String sql = """
            SELECT * FROM applications 
            WHERE last_used IS NULL 
               OR datetime(last_used) < datetime('now', ?)
            ORDER BY last_used ASC, name ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "-" + days + " days");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    apps.add(mapResultSetToApp(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }

    public List<String> getCategoriesList() {
        List<String> list = new ArrayList<>(List.of(
            "Development", "Browsers", "Productivity", "Media", "Gaming", 
            "Communication", "Utilities", "Education", "Others"
        ));
        String sql = "SELECT DISTINCT category FROM applications WHERE category IS NOT NULL AND category != ''";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String cat = rs.getString("category");
                if (!list.contains(cat)) {
                    list.add(cat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void delete(int id) {
        String sql = "DELETE FROM applications WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ApplicationInfo mapResultSetToApp(ResultSet rs) throws SQLException {
        return new ApplicationInfo(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("publisher"),
            rs.getString("version"),
            rs.getString("install_location"),
            rs.getString("install_date"),
            rs.getLong("size_bytes"),
            rs.getString("executable_path"),
            rs.getString("icon_path"),
            rs.getString("category"),
            rs.getInt("is_favorite") == 1,
            rs.getInt("launch_count"),
            rs.getString("last_used")
        );
    }
}
