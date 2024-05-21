package com.example.bookservice.entity.genres;

import com.example.bookservice.entity.GenreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Detective implements GenreInterface {

    private static final String name = "Detective";

    @Autowired
    public Detective(GenreFactory genreFactory) {
        genreFactory.registerGenre(name, this);
    }

    @Override
    public String getName() {
        return name;
    }
}
