package com.vbforge.educationapi.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course extends BaseEntity{

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 100)
    @Column(length = 100)
    private String instructor;

    @Size(max = 5000)
    @Column(length = 5000)
    private String syllabus;

    @Size(max = 500)
    @Column(length = 500)
    private String schedule;

    //one course --> many modules
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Module> modules = new ArrayList<>();

    //one course --> many enrollments
    @OneToMany(mappedBy = "course",  cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();


}

















