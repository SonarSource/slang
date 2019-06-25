# sonar-go-to-slang

Generate slang serialized AST in JSON from a go source file.

## Building

To generate `goparser_generated.go` file in current directory, run:

    go generate

To create `sonar-go-to-slang` executable in current directory, run:

    go build

To create `sonar-go-to-slang` executable in `$GOPATH/bin`, run:

    go install

## Running

If you have `$GOPATH/bin` on your `PATH`, it's easy to run with `slang-generator-go`.

Run with `-h` or `-help` or `--help` to get usage help.

Print the SLANG Json tree for some `source.go`:

    sonar-go-to-slang source.go

Dump the native raw AST for some `source.go`:

    sonar-go-to-slang -d source.go
