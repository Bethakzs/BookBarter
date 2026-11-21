package com.example.bookservice.entity;

import com.example.bookservice.entity.genres.Detective;
import com.example.bookservice.entity.genres.Novel;
import com.example.bookservice.entity.genres.SciFi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenreFactoryTest {

    private GenreFactory genreFactory;

    @BeforeEach
    void setUp() {
        genreFactory = new GenreFactory();
        // genre beans register themselves in their constructors
        new Novel(genreFactory);
        new SciFi(genreFactory);
        new Detective(genreFactory);
    }

    @Test
    void translateGenre_isCaseInsensitive() {
        assertThat(genreFactory.translateGenre("NOVEL")).isEqualTo("Novel");
        assertThat(genreFactory.translateGenre("scifi")).isEqualTo("SciFi");
    }

    @Test
    void translateGenre_list_preservesOrder() {
        assertThat(genreFactory.translateGenre(List.of("detective", "Novel"))).containsExactly("Detective", "Novel");
    }

    @Test
    void translateGenre_unknownGenre_throws() {
        assertThatThrownBy(() -> genreFactory.translateGenre("cookbook"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unexpected value: cookbook");
        assertThatThrownBy(() -> genreFactory.translateGenre(List.of("novel", "cookbook")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
