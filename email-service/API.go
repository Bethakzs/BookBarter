package main

import (
	"github.com/gin-gonic/gin"
)

func StartAPI() {
	r := gin.Default()

	startPingChecker()

	err := r.Run(":8089")
	if err != nil {
		return
	}
}
