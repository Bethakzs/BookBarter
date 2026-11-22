-- Адмін-акаунт BookBarter за замовчуванням. Spring виконує цей файл при кожному старті user-service
-- (spring.sql.init.mode=always); якщо користувач уже існує — нічого не змінюється.
-- Логін: romanwdwd@gmail.com (username: roman). Пароль зберігається як BCrypt-хеш (як у auth-service).

INSERT INTO users (username, email, password, mobile_phone, rating, image, buck, refresh_token, notifications)
VALUES ('roman', 'romanwdwd@gmail.com', '$2a$10$m1nSRJtoqiqXcQnyOCcpK.DUyoI9ulb6vmFmaGrKyy0fjuDClgxO.', '+380000000000', 5.0, NULL, 100, NULL, false)
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT u.id, r.role
FROM users u
CROSS JOIN (VALUES ('ROLE_USER'), ('ROLE_ADMIN')) AS r(role)
WHERE u.email = 'romanwdwd@gmail.com'
ON CONFLICT DO NOTHING;
