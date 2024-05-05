package API

import (
	"github.com/gin-gonic/gin"
)

func StartAPI() {
	r := gin.Default()

	startPingChecker()

	err := r.Run(":8088")
	if err != nil {
		return
	}
}
