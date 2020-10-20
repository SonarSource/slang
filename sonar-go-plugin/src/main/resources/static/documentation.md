---
title: Go
key: go
---

<!-- static -->
<!-- update_center:go -->
<!-- /static -->



## Prerequisites

* SonarQube Scanner should run on a x86-64 Windows, macOS or Linux 64bits machine
* You need the [Go](https://golang.org/) installation on the scan machine only if you want to import coverage data

## Language-Specific Properties

You can discover and update the Go-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Go](/#sonarqube-admin#/admin/settings?category=go)**

By default, all the `vendor` directories are excluded from the analysis. However, you can change the property `sonar.go.exclusions` to a different pattern if you want to force their analysis (not recommended).

## "sonar-project.properties" Sample

Here is a first version of a `sonar-project.properties` file, valid for a simple `Go` project:

```
  sonar.projectKey=com.company.projectkey1
  sonar.projectName=My Project Name

  sonar.sources=.
  sonar.exclusions=**/*_test.go

  sonar.tests=.
  sonar.test.inclusions=**/*_test.go
```

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/)
* [Importing External Issues](/analysis/external-issues/) (GoVet, GoLint, GoMetaLinter)
