package com.observability.userservice.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    @Column(name = "username", nullable = false)
    private String userName;
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public User() {
    }

    public User(String userName, String email) {
        this.userName = userName;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId != null && userId.equals(user.userId); //Adjusted from default because an unsaved entity would be NULL and two objects would return true.
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
