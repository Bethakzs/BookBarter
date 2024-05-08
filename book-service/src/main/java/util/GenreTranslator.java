package util;

import com.example.bookservice.entity.Genre;
import org.springframework.stereotype.Component;

@Component
public class GenreTranslator {

    public String translate(Genre genre) {
        return switch (genre) {
            case NOVEL -> "Роман";
            case POETRY -> "Поезія";
            case FANTASY -> "Фантастика";
            case FICTION -> "Художня література";
            case DETECTIVE -> "Детектив";
            case BIOGRAPHY -> "Біографія";
            case HISTORICAL -> "Історичний";
            case SCIENTIFIC -> "Науковий";
            case SCI_FI -> "Наукова фантастика";
            case CHILDREN -> "Дитячий";
            default -> "Невідомий жанр";
        };
    }
}
