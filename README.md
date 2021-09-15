springbootsampleapp
==================

**Description of your service here**

## API Specification
Documentation of the API for this service can be found in the [API](api.yaml) document. The API follows
[sample's API specification](https://confluence.sample.com/display/NEA/API+Standards) and is defined by the
[OpenAPI v3 format](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md). This service has been
initialized with a barebones API, but complete templates that conform to the sample standards are available [here](https://confluence.sample.com/display/NEA/OpenAPI+v3+Templates)
Once an API is defined a new appliction can be created on [sample's internal developer portal](https://developer.sampletech.com/)
which will point to this project's `api.yaml` file.

## Versioning
springbootsampleapp uses [Semantic Versioning](http://semver.org) to convey information about changes.  For a detailed list of
changes in each release, please consult the [Version History](CHANGES.md).

## Building The Service
The service can be built with the following commands:

```
$ ./gradlew clean build
```

## Running The Service
The service contains a **run.sh** script that can be used to start the application.

```
$ ./run.sh
```

## Debugging The Service
The service contains a **debug.sh** script that can be used to start the application in debugging mode.  The application
will wait until you connect a debugger on port **5005** to start.

```
$ ./debug.sh
```

## Pipeline
### Deployment
After editing *application.yaml* so that all templated values in square brackets have been updated, execute the following:

```
keystone -u [ntusername] --force ./src/main/resources/application.yaml 
```

### Integration Testing
This blueprint has a gradle STS SQS integration test example that you can easily check out after building the *03.1_(TEST)_integration-test* job.

To see the test results, open up the following url after replacing square brackets (i.e. *Workspace/build/reports/tests/index.html* > *com.sample.springbootsampleapp.ApplicationTests* > *Tests* > *testExampleController_SQS*):

```
https://[your_brewmaster].tools.samplecloud.com/job/[application.yaml:application.name]/job/Test/job/03.1_(TEST)_integration-test/ws/build/reports/tests/classes/com.sample.springbootsampleapp.ApplicationTests.html#tab1
```

The *testExampleController_SQS* test should be passing with a *Standard output* similar to the following:

```
2014394534460390606 INFO  2016-10-13 17:27:29,069 [XNIO-3 task-8] com.sample.springbootsampleapp.controller.ExampleController app=springbootsampleapp version=2.0.1.18 : Creating a new SQS queue called continuous_integration-blueprint-springboot
2014394534460390606 INFO  2016-10-13 17:27:30,416 [XNIO-3 task-8] com.sample.springbootsampleapp.controller.ExampleController app=springbootsampleapp version=2.0.1.18 : Sending a message to continuous_integration-blueprint-springboot
2014394534460390606 INFO  2016-10-13 17:27:30,575 [XNIO-3 task-8] com.sample.springbootsampleapp.controller.ExampleController app=springbootsampleapp version=2.0.1.18 : Receiving messages from continuous_integration-blueprint-springboot
2014394534460390606 INFO  2016-10-13 17:27:30,674 [XNIO-3 task-8] com.sample.springbootsampleapp.controller.ExampleController app=springbootsampleapp version=2.0.1.18 : Deleting message to continuous_integration-blueprint-springboot
2014394534460390606 INFO  2016-10-13 17:27:30,765 [XNIO-3 task-8] com.sample.springbootsampleapp.controller.ExampleController app=springbootsampleapp version=2.0.1.18 : Deleting the queue continuous_integration-blueprint-springboot
```

## API Error Handling

This blueprint uses sample's open source [Backstopper](https://github.com/sample-Inc/backstopper) libraries to provide a 
production-ready API error creation and handling system that is consistent for callers, easy to debug errors when 
consumers report them, and does not leak any information (i.e. no exception stack traces or messages, class names, etc).

To use Backstopper to its fullest potential simply add errors to the `ProjectApiError` enum and either throw them with 
`ApiException` or take advantage of the JSR 303 Java Bean Validation support to have model object validation errors 
automatically converted to the sanitized client errors you specify in `ProjectApiError`.

See the Backstopper Github readme's [barebones example](https://github.com/sample-Inc/backstopper#barebones-example-assumes-framework-integration-is-already-done)
and [usage quickstart](https://github.com/sample-Inc/backstopper#quickstart---usage) sections for a primer on using
the error handling system. [This comparison article](https://confluence.sample.com/pages/viewpage.action?pageId=181366686)
also has some good info on why Backstopper is helpful and a little on how to use some of its features. 

If you have any questions there is a `#cop-backstopper` Slack channel that is happy to help you out.