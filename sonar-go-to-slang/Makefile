GO_VERSION=1.19.1
GO_BINARY=go$(GO_VERSION)

all: goparser_generated.go

install_go:
	go install golang.org/dl/go"${GO_VERSION}@latest"
	$(GO_BINARY) download


goparser_generated.go: generate_source.go install_go
	$(GO_BINARY) run generate_source.go
	GOOS=darwin GOARCH=amd64 $(GO_BINARY) build -o build/sonar-go-to-slang-darwin-amd64
	GOOS=linux GOARCH=amd64 $(GO_BINARY) build -o build/sonar-go-to-slang-linux-amd64
	GOOS=windows GOARCH=amd64 $(GO_BINARY) build -o build/sonar-go-to-slang-windows-amd64.exe

test-report.out: install_go
	$(GO_BINARY) test -json > test-report.out


clean:
	rm -f goparser_generated.go
	rm -f build/sonar-go-to-slang-*
