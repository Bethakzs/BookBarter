-- Тестові книги BookBarter. Spring виконує цей файл при кожному старті book-service
-- (spring.sql.init.mode=always), але дані додаються лише в порожню (нову) базу.

INSERT INTO books (title, image, description, author, year, published_by, price, status, user_email)
SELECT v.title, NULL, v.description, v.author, v.year, v.published_by, v.price, 'AVAILABLE', 'romanwdwd@gmail.com'
FROM (VALUES ('The Lord of the Rings', 'Epic fantasy adventure', 'J.R.R. Tolkien', 1954, 'Allen & Unwin', 10),
             ('Dune', 'Science fiction masterpiece', 'Frank Herbert', 1965, 'Chilton Books', 9),
             ('The Hobbit', 'Prequel to LOTR', 'J.R.R. Tolkien', 1937, 'Allen & Unwin', 3),
             ('1984', 'Dystopian classic', 'George Orwell', 1949, 'Secker & Warburg', 13),
             ('To Kill a Mockingbird', 'Story of racial injustice', 'Harper Lee', 1960, 'J.B. Lippincott', 10),
             ('The Da Vinci Code', 'Religious conspiracy thriller', 'Dan Brown', 2003, 'Doubleday', 8),
             ('Pride and Prejudice', 'Romance and manners', 'Jane Austen', 1813, 'T. Egerton', 9),
             ('The Martian', 'Survival on Mars', 'Andy Weir', 2011, 'Crown', 2),
             ('The Shining', 'Haunted hotel horror', 'Stephen King', 1977, 'Doubleday', 20),
             ('Harry Potter and the Philosopher''s Stone', 'Wizarding world debut', 'J.K. Rowling', 1997, 'Bloomsbury', 30))
     AS v(title, description, author, year, published_by, price)
WHERE NOT EXISTS (SELECT 1 FROM books);

INSERT INTO book_genre (book_id, genre)
SELECT b.id, g.genre
FROM books b
JOIN (VALUES ('The Lord of the Rings', 'Fantasy'),
             ('Dune', 'Fiction'),
             ('The Hobbit', 'Children'),
             ('1984', 'Fiction'),
             ('To Kill a Mockingbird', 'Fiction'),
             ('The Da Vinci Code', 'Fantasy'),
             ('Pride and Prejudice', 'Historical'),
             ('The Martian', 'Novel'),
             ('The Shining', 'Fiction'),
             ('Harry Potter and the Philosopher''s Stone', 'Fantasy'))
     AS g(title, genre) ON g.title = b.title
WHERE NOT EXISTS (SELECT 1 FROM book_genre);
