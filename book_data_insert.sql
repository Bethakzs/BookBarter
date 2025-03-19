TRUNCATE TABLE books CASCADE;
ALTER SEQUENCE books_id_seq RESTART WITH 1;

INSERT INTO books (title, image, description, author, year, published_by, price, status, user_email)
VALUES ('The Lord of the Rings', NULL, 'Epic fantasy adventure', 'J.R.R. Tolkien', 1954, 'Allen & Unwin', 10, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('Dune', NULL, 'Science fiction masterpiece', 'Frank Herbert', 1965, 'Chilton Books', 9, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('The Hobbit', NULL, 'Prequel to LOTR', 'J.R.R. Tolkien', 1937, 'Allen & Unwin', 3, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('1984', NULL, 'Dystopian classic', 'George Orwell', 1949, 'Secker & Warburg', 13, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('To Kill a Mockingbird', NULL, 'Story of racial injustice', 'Harper Lee', 1960, 'J.B. Lippincott', 10, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('The Da Vinci Code', NULL, 'Religious conspiracy thriller', 'Dan Brown', 2003, 'Doubleday', 8, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('Pride and Prejudice', NULL, 'Romance and manners', 'Jane Austen', 1813, 'T. Egerton', 9, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('The Martian', NULL, 'Survival on Mars', 'Andy Weir', 2011, 'Crown', 2, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('The Shining', NULL, 'Haunted hotel horror', 'Stephen King', 1977, 'Doubleday', 20, 'AVAILABLE', 'romanwdwd@gmail.com'),
       ('Harry Potter and the Philosopher''s Stone', NULL, 'Wizarding world debut', 'J.K. Rowling', 1997, 'Bloomsbury', 30, 'AVAILABLE', 'romanwdwd@gmail.com');

TRUNCATE TABLE book_genre;

INSERT INTO book_genre (book_id, genre)
VALUES (1, 'Fantasy'),
       (2, 'Fiction'),
       (3, 'Children'),
       (4, 'Dystopian'),
       (5, 'Fiction'),
       (6, 'Fantasy'),
       (7, 'Historical'),
       (8, 'Novel'),
       (9, 'Fiction'),
       (10, 'Fantasy');
