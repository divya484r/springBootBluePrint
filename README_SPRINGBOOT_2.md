SpringBoot 2
============

This branch contains the changes necessary to upgrade to SpringBoot 2. 

For information about what's new, see:
- [SpringBoot 2.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Release-Notes)
- [SpringBoot 2.1.0 Release Notes](https://spring.io/blog/2018/10/30/spring-boot-2-1-0)
- [SpringCloud Greenwich Release](https://spring.io/blog/2019/01/23/spring-cloud-greenwich-release-is-now-available)

Note: As part of the SpringCloud Greenwich Release, they announced: 

> The following Spring Cloud Netflix modules and corresponding starters will be placed into maintenance mode:
>
> spring-cloud-netflix-archaius  
> spring-cloud-netflix-hystrix-contract  
> spring-cloud-netflix-hystrix-dashboard  
> spring-cloud-netflix-hystrix-stream  
> spring-cloud-netflix-hystrix  
> spring-cloud-netflix-ribbon  
> spring-cloud-netflix-turbine-stream  
> spring-cloud-netflix-turbine  
> spring-cloud-netflix-zuul  
 
## SpringBoot 1.x
The EOL for SpringBoot 1.x is coming up in August 2019. Until that time, if you need to build a new service with SB1,
you can use the `legacy/springboot1` branch (or `springboot1` Git tag) to do so. 

### Upgrading an existing service
If you're upgrading an existing service from SpringBoot 1.x, you can view the changes in the Blueprint since the SB2
changes have been merged, by viewing the diff of the `legacy/springboot1` branch & `master`: [Branch diff](https://bitbucket.sample.com/projects/COPBLUE/repos/blueprint-java-springboot/compare/commits?sourceBranch=refs%2Fheads%2Flegacy%2Fspringboot1&targetRepoId=27747)

The PR which brought in the initial upgrade was [PR 107](https://bitbucket.sample.com/projects/COPBLUE/repos/blueprint-java-springboot/pull-requests/107/diff)
and the majority of the Spring specific changes were done as part of [this commit](https://bitbucket.sample.com/projects/COPBLUE/repos/blueprint-java-springboot/commits/8f5994b5ecb40823b93ac8e9102f990013956150).


# Change Log
Details on how to contribute to this changelog see the website
[Keep a change Log.](http://keepachangelog.com/) All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

> Given a version number MAJOR.MINOR.PATCH, increment the:  
> MAJOR version when you make incompatible API changes,  
> MINOR version when you add functionality in a backwards-compatible manner, and  
> PATCH version when you make backwards-compatible bug fixes.

### [0.1.0]
#### Changed
- Update project to SpringBoot 2.1
- Update SpringCloud dependency to Greenwich.RELEASE 

### [0.0.1]
#### Changed
- Update README to include notes on Experimental status
- Upgrade project to SpringBoot 2.0
    - build.gradle:
        - update SpringBoot dependency to 2.0.2.RELEASE
        - update SpringCloud dependency to Finchley.RELEASE
        - update Eureka dependency to use renamed starter library
        - use new Spring dependency management plugin
        - (optional) add migrator dependency (runtime) to give better error messages if using deprecated properties
        - update renamed mainClass property reference
    - tests:
        - update deprecated port property reference in integration tests
        - update reference to moved annotation LocalServerPort
        - add releaseVersion property source to ManagementEndpointTests (due to strict parsing)
    - application.properties:
        - update deprecated property management.port -> management.server.port
        - update deprecated property server.contextPath -> server.servlet.contextPath
        - update eureka properties to use updated property names
        - set management endpoints URI prefix to root (`/`) instead of SpringBoot 2.0 default (`/actuator`) - allowing existing infrastructure to continue to find /info and /health endpoints