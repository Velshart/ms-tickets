package me.mmtr.notificationservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_ids_emails")
public class UserIdEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "user_id_email_seq",
            sequenceName = "user_id_email_id_seq",
            allocationSize = 100
    )
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    public UserIdEmail() {}

    public UserIdEmail(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
