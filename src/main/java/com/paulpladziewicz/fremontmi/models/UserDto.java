package com.paulpladziewicz.fremontmi.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Data
@Document(collection = "users")
public class UserDto {
    @Id
    private String id;

    private String username;

    private String password;

    private String resetPasswordToken;

    private final Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

    private final boolean accountNonExpired = true;

    private final boolean accountNonLocked = true;

    private final boolean credentialsNonExpired = true;

    private final boolean enabled = true;
}
