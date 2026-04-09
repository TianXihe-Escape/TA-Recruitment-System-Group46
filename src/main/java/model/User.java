package model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Login account persisted in the file store.
 */
public class User {
    private String userId;
    private String name;
    private String username;
    private String password;
    private Role role;
    private List<String> managedModuleCodes = new ArrayList<>();

    public User() {
    }

    public User(String userId, String name, String username, String password, Role role) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(String userId, String username, String password, Role role) {
        this(userId, null, username, password, role);
    }

    public User(String userId, String username, String password, Role role, List<String> managedModuleCodes) {
        this(userId, username, password, role);
        setManagedModuleCodes(managedModuleCodes);
    }

    public User(String userId, String name, String username, String password, Role role, List<String> managedModuleCodes) {
        this(userId, name, username, password, role);
        setManagedModuleCodes(managedModuleCodes);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<String> getManagedModuleCodes() {
        return new ArrayList<>(managedModuleCodes);
    }

    public void setManagedModuleCodes(List<String> managedModuleCodes) {
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        if (managedModuleCodes != null) {
            for (String code : managedModuleCodes) {
                if (code != null) {
                    String normalized = code.trim().toUpperCase(Locale.ROOT);
                    if (!normalized.isBlank()) {
                        normalizedCodes.add(normalized);
                    }
                }
            }
        }
        this.managedModuleCodes = new ArrayList<>(normalizedCodes);
    }

    public boolean managesModule(String moduleCode) {
        if (moduleCode == null) {
            return false;
        }
        String normalized = moduleCode.trim().toUpperCase(Locale.ROOT);
        return managedModuleCodes.contains(normalized);
    }
}
