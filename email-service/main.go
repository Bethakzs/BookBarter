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

	instance := eureka.NewInstanceInfo(ip, "email-service", "email-service", 8089, 30, false)
	client.RegisterInstance("email-service", instance)

	topics := []string{"email-service-request-buy-book", "email-service-request-sell-book", "email-service-request-register"}
	groupID := "email-service"

	for _, topic := range topics {
		go startConsumer(topic, groupID)
	}

	go API.StartAPI()

	select {}
}
