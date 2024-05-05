package main

import (
	"context"
	"github.com/segmentio/kafka-go"
	"log"
	"net/smtp"
	"strings"
)

func startConsumer(topic string, groupID string) {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:  []string{"localhost:9092"},
		GroupID:  groupID,
		Topic:    topic,
		MinBytes: 10e3,
		MaxBytes: 10e6,
	})

	for {
		m, err := r.ReadMessage(context.Background())
		if err != nil {
			break
		}
		log.Printf("topic: %s, message: %s", m.Topic, string(m.Value))
		switch topic {
		case "notification-service-request-buy-book":
			parts := strings.Split(string(m.Value), ":")
			if len(parts) < 2 {
				log.Printf("Invalid message format: %s", string(m.Value))
				break
			}
			buyerEmail := parts[0]
			bookTitle := parts[1]
			message := "<h1>Ви успішно замовили книгу " + "'" + bookTitle + "'" + "</h1><p>Тепер продавець зв'яжеться з вами для уточнення доставки, очікуйте.</p><p><b>Примітка:</b> Якщо продавець не зв'яжеться з вами протягом 3 днів, зверніться до служби підтримки. Якщо ви відміните замовлення, ви втратите свої гроші. Не підтверджуйте отримання книги, якщо ви ще не отримали її. Дякуємо за замовлення " + bookTitle + "!</p>"
			sendHTMLMessage(buyerEmail, "Notification Service", message)
		case "notification-service-request-sell-book":
			parts := strings.Split(string(m.Value), ":")
			if len(parts) < 3 {
				log.Printf("Invalid message format: %s", string(m.Value))
				break
			}
			sellerEmail := parts[0]
			buyerPhone := parts[1]
			buyerEmail := parts[2]
			message := "<h1>Вашу книгу було замовлено!</h1><p>Покупець зв'яжеться з вами за допомогою наступних контактних даних:</p><p>Телефон: " + buyerPhone + "</p><p>Email: " + buyerEmail + "</p><p><b>Примітка:</b> Якщо покупець не зв'яжеться з вами протягом 3 днів, зверніться до служби підтримки. Дякуємо за використання нашого сервісу обміну книгами!</p>"
			sendHTMLMessage(sellerEmail, "Notification Service", message)
		case "notification-service-request-register":
			sendHTMLMessage(string(m.Value), "Notification Service", "<h1>Вітаємо!</h1><p>Ви успішно зареєструвалися в нашому сервісі обміну книгами! Ми раді, що ви приєдналися до нас. Тут ви можете знайти багато цікавих книг від інших користувачів та поділитися своїми. Якщо у вас виникнуть питання, будь ласка, зверніться до служби підтримки. Дякуємо, що ви з нами!</p>")
		}
	}

	err := r.Close()
	if err != nil {
		return
	}
}

func sendHTMLMessage(to string, subject string, body string) {
	from := "boxride228@gmail.com"
	pass := "cwqk tosx ttre zvgl"
	toEmail := to

	msg := "From: " + from + "\n" +
		"To: " + toEmail + "\n" +
		"Subject: " + subject + "\n" +
		"MIME-version: 1.0;\nContent-Type: text/html; charset=\"UTF-8\";\n\n" +
		body

	err := smtp.SendMail("smtp.gmail.com:587",
		smtp.PlainAuth("", from, pass, "smtp.gmail.com"),
		from, []string{toEmail}, []byte(msg))

	if err != nil {
		log.Printf("smtp error: %s", err)
		return
	}

	log.Print("Email sent")
}
