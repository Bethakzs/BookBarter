# Multi-stage збірка Go-сервісу: компіляція в golang, запуск у мінімальному alpine
FROM golang:1.21 AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY *.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -o /email-service

FROM alpine:3.20
RUN adduser -S -u 1001 app
COPY --from=build /email-service /usr/local/bin/email-service
USER app
EXPOSE 8089
CMD ["/usr/local/bin/email-service"]
