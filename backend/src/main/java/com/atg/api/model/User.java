package com.atg.api.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class User {

    public enum Role { USER, ADMIN }
    public enum Membership { NONE, DERIV_VIP, WELTRADE_VIP }

    private Long id;

    private String name;
    private String username;
    private String country;
    private String email;
    private String phone;
    private String passwordHash;
    private String avatarUrl;

    private Role role = Role.USER;

    private Membership membership = Membership.NONE;

    /** When the current VIP membership lapses; null means it never expires. */
    private LocalDateTime membershipExpiresAt;

    private int points = 0;
    private int tokenVersion = 0;
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getDisplayName() {
        return username == null || username.isBlank() ? name : username;
    }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Membership getMembership() { return membership; }
    public void setMembership(Membership membership) { this.membership = membership; }
    public LocalDateTime getMembershipExpiresAt() { return membershipExpiresAt; }
    public void setMembershipExpiresAt(LocalDateTime membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
