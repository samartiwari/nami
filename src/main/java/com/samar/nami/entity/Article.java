package com.samar.nami.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(unique = true, length = 512)
    private String url;

    @Column(length = 1000)
    private String snippet;

    private int totalWords;

    public Article(String title, String url, String snippet, int totalWords) {
        this.title = title;
        this.url = url;
        this.snippet = snippet;
        this.totalWords = totalWords;
    }
}
