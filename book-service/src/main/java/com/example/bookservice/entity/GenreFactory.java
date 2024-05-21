package com.example.bookservice.entity;

import com.example.bookservice.entity.genres.GenreInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GenreFactory {

    private final Map<String, GenreInterface> genreMap = new HashMap<>();


    public void registerGenre(String genre, GenreInterface genreObject) {
        genreMap.put(genre.toLowerCase(), genreObject);
    }

    public String translateGenre(String genre) {
        GenreInterface genreObject = genreMap.get(genre.toLowerCase());
        if (genreObject == null) {
            throw new IllegalArgumentException("Unexpected value: " + genre);
        }
        return genreObject.getName();
    }

    public List<String> translateGenre(List<String> genres) {
        ArrayList<String> genreList = new ArrayList<>();
        for (String genre : genres) {
            GenreInterface tempGenre = genreMap.get(genre.toLowerCase());
            if (tempGenre == null) {
                throw new IllegalArgumentException("Unexpected value: " + genre);
            }
            genreList.add(tempGenre.getName());
        }
        return genreList;
    }
}