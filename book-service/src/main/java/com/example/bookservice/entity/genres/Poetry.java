package com.example.bookservice.entity.genres;

import com.example.bookservice.entity.GenreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Poetry implements GenreInterface {

    private static final String name = "Poetry";

    @Autowired
    public Poetry(GenreFactory genreFactory) {
        genreFactory.registerGenre(name, this);
    }

    @Override
    public String getName() {
        return name;
    }
}