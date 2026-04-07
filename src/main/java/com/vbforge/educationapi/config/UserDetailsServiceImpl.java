package com.vbforge.educationapi.config;

import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Student student = studentRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException("No user found with email: " + email));

        return new User(
                student.getEmail(),
                student.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + student.getRole().name()))
                // ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN
        );
    }
}
