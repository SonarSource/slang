// S105
package samples

import (
	"crypto/tls"
	"fmt"
	"net/http"
)

func httpsClient() {
	http.DefaultTransport.(*http.Transport).TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	_, err := http.Get("https://golang.org/")
	if err != nil {
		fmt.Println(err)
	}
}
