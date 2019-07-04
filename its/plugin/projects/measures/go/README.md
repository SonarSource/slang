# Test Report

The file `go-test-report.out` has been generated in this context:

* go version 1.10
* GOPATH defined so the slang git repository is in $GOPATH/src/github.com/SonarSource/slang

Using the command:
```
go1.10 test -json | \
 sed 's|github.com/SonarSource/slang/its/plugin/projects/measures/go|samples|g' > go-test-report.out
```
