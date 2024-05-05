package main

import (
	"BookBarter/API"
	_ "fmt"
	"github.com/ArthurHlt/go-eureka-client/eureka"
	"net"
)

func main() {
	client := eureka.NewClient([]string{
		"http://localhost:8761/eureka",
	})

	addrs, err := net.InterfaceAddrs()
	if err != nil {
		panic(err)
	}
	ip := addrs[0].(*net.IPNet).IP.String()

	instance := eureka.NewInstanceInfo(ip, "notification-service", "notification-service", 8088, 30, false)
	client.RegisterInstance("notification-service", instance)

	topics := []string{"notification-service-request-buy-book", "notification-service-request-sell-book", "notification-service-request-register"}
	groupID := "notification-service"

	for _, topic := range topics {
		go startConsumer(topic, groupID)
	}

	go API.StartAPI()

	select {}
}
