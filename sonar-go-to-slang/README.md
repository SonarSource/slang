# sonar-go-to-slang

Generate slang serialized AST in JSON from a go source file.

## Building

To generate `goparser_generated.go` file in current directory, run:

    go generate

To create `sonar-go-to-slang` executable in current directory, run:

    go build

To create `sonar-go-to-slang` executable in `$GOPATH/bin`, run:

    go install
    
### Building on Windows

When trying to build `sonar-go-to-slang` on Windows, the build may fail with the following error:

     > Create symbolic link at [...]\slang\sonar-go-to-slang\.gogradle\project_gopath\src\github.com\SonarSource\slang\sonar-go-to-slang failed
     
Creating the symbolic link by hand solves this problem:

* (Eventually enable [developer mode in Windows](https://docs.microsoft.com/en-us/windows/uwp/get-started/enable-your-device-for-development))

* Run (in `sonar-go-to-slang` folder):


     mklink /D ".gogradle\project_gopath\src\github.com\SonarSource\slang\sonar-go-to-slang" "Absolute\Path\To\slang\sonar-go-to-slang"


## Running

If you have `$GOPATH/bin` on your `PATH`, it's easy to run with `slang-generator-go`.

Run with `-h` or `-help` or `--help` to get usage help.

Print the SLANG Json tree for some `source.go`:

    sonar-go-to-slang source.go

Dump the native raw AST for some `source.go`:

    sonar-go-to-slang -d source.go
    
## Testing

To perform the tests, run:

    go test
    
To update test results, use the method `fix_all_go_files_test_automatically` in `goparser_test.go`
