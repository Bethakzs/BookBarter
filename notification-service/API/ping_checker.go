package API

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"net/http"
	"strconv"
	"time"
)

func startPingChecker() {
	r := gin.Default()

	// Existing endpoint
	r.GET("/pingtest", func(c *gin.Context) {
		servers := map[string]string{
			"USA":         "http://google.com",
			"France":      "http://lemonde.fr",
			"Japan":       "http://asahi.com",
			"SouthAfrica": "http://news24.com",
			"Australia":   "http://abc.net.au",
			"Canada":      "http://cbc.ca",
			"Germany":     "http://bild.de",
			"China":       "http://sina.com.cn",
			"Brazil":      "http://uol.com.br",
			"India":       "http://timesofindia.indiatimes.com",
			"Russia":      "http://yandex.ru",
			"Mexico":      "http://unotv.com",
			"Italy":       "http://corriere.it",
			"Spain":       "http://elpais.com",
			"Turkey":      "http://hurriyet.com.tr",
			"Sweden":      "http://dn.se",
			"Nigeria":     "http://punchng.com",
			"Argentina":   "http://clarin.com",
			"Poland":      "http://wp.pl",
			"Norway":      "http://vg.no",
			"South Korea": "http://chosun.com",
			"Indonesia":   "http://detik.com",
			"Netherlands": "http://telegraaf.nl",
			"Switzerland": "http://blick.ch",
			"Belgium":     "http://hln.be",
			"Egypt":       "http://youm7.com",
		}
		results := make(map[string]float64)

		for country, server := range servers {
			start := time.Now()
			_, err := http.Get(server)
			if err != nil {
				c.JSON(500, gin.H{
					"message": "Failed to ping server in " + country,
				})
				return
			}
			duration := time.Since(start).Seconds() * 1000 // Convert to milliseconds
			durationStr := fmt.Sprintf("%.1f", duration)
			durationFloat, err := strconv.ParseFloat(durationStr, 64)
			if err != nil {
				c.JSON(500, gin.H{
					"message": "Failed to convert duration to float64",
				})
				return
			}
			results[country] = durationFloat
		}

		c.JSON(200, gin.H{
			"ping": results,
		})
	})

	err := r.Run(":8088")
	if err != nil {
		return
	}
}
