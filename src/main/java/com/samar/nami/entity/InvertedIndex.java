package com.samar.nami.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    indexes = @Index(name = "idx_word", columnList = "word"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"word", "articleId"})
)
public class InvertedIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private int count;

    public InvertedIndex(String word, Long articleId, int count) {
        this.word = word;
        this.articleId = articleId;
        this.count = count;
    }
}
