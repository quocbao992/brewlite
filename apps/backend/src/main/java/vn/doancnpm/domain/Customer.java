package vn.doancnpm.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Builder 
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class Customer {
    
    @Id 
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne 
    @JoinColumn(name = "user_id",referencedColumnName = "id",unique = true,nullable = false)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate date;

    @Column(name = "gender")
    private boolean gender;

    @Column(name = "phone_number", unique = true, length = 10)
    private String phone;

    @Builder.Default
    @Column (name = "loyalty_point")
    private float loyaltyP=0;
}
