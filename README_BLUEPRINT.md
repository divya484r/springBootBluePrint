blueprint-java-springboot
=========================

# Overview
This project is a fork of the original `blueprint-springboot` template, removing much of the example code and dependencies
on various libraries. The goal of this project is to provide only the bare minimum to create a new springboot microservice,
without making assumptions about the desired frameworks or requirements of the service.
 
The `master` branch of this project contains code for a Eureka discoverable REST microservice.

To opt-in to other features, after cloning this repo you can merge in any of the feature branches in this repo. These features
include S3, DynamoDB, Ribbon, Hystrix, etc. Check the project's current list of branches for the list of features:

> `git branch -r`

Preview changes from feature branches by merging without committing. You can then diff, alter, commit, or revert.

> Example: `git pull origin feature/s3 --no-ff --no-commit`

If you prefer to preview the changes in Bitbucket:

  1. Opening this repo in Bitbucket
  2. Select the Branches option in the sidebar
  3. Click the feature branch you want to preview


# TL;DR
To quickly get started, watch these 30 second screen captures on how this repo can be used: [Video series](https://confluence.sample.com/display/DEVCOM/viewpage.action?pageId=189114262#UserManual-Videoseries)

# User Manual

## Usage patterns
There are a few options for using this project.

#### 1. Minimal microservice, no additional features
The first and simplest option is to use the master branch. This is best for people looking to get started with
a minimal microservice. Just follow the directions under [How To Use](#how-to-use), skipping step 3.

#### 2. Microservice with extra features
The second option involves merging in some features. This is useful if you have an idea of the libraries and
datastores your project will need. Just follow the directions under [How To Use](#how-to-use), merging in each
feature branch you want to include in your project, eg:

> `git pull origin feature/s3`

After creating your service, look through the README files of the features you merged in (eg, README_S3.md)
to see if there are any manual steps required before deploying to TEST.

#### 3. Microservice with from custom blueprint
The third option is how you and your team can make the blueprint your own. Go to the project in Bitbucket
and fork it into the Bitbucket space your team keeps their repos. For clarity, add your team name to your
fork name (eg, `{team}-springboot-blueprint`). Make sure the 'fork-sync' option in enabled.

From now on, when creating a new service, clone your team fork when following the directions under [How To Use](#how-to-use).
Updates to the original repo will automatically be pushed to your team's fork, as long as fork syncing is
enabled and you don't modify the branches inherited from the original repo. See [fork-sync explained](#fork-sync-explained) below for more details.

To customize the blueprint, create new branches on your team repo. Think of these branches as 'overlays' that
will be applied on the `master` branch. You can now capture common service features or patterns that you want
to use as a starting point for new services. Teams can also create a branch on their fork containing team-specific
config changes (such as updates to the application.yaml for team email, jenkins URL, etc) that can be merged
when creating a new service.

Maintaining a team fork gives you the flexibility of experimenting with new templated patterns while still
giving you the most up-to-date changes from the original repo!


## <a name="how-to-use"></a> How to use
To create a new service using this repo:

1. Create a new repo in your Bitbucket space 
2. Clone this project to the directory you want your service (`git clone {this-repo} {service-name}`)
    > IMPORTANT: service names are limited by Asgard to lowercase and up to 20 characters `^[a-z0-9]{0,20}$`  
    > Example: `git clone ssh://git@bitbucket.sample.com/copblue/blueprint-java-springboot.git testservice`

3. Merge any of the desired feature branches in your `master` branch.
4. Run the `init.groovy` (ie, `groovy init.groovy`) to rename the placeholder text in the project. This script will ask for the
name of your new service, your Bitbucket project key (eg, CEIC, CEFL, PHYLINV, etc), the domain name of your service, etc.
5. Push to your Bitbucket repo

After creating the repo, you'll want to manually look over the following files:

* src/main/resources/application.yaml - there is some placeholder text that needs updating (wrapped by square brackets [])
* src/main/resources/application.properties - (optional) if you need to add underscores to your base URI, update the `contextPath` and `virtualHostName` properties
* gradle/fortify.gradle - update the `FORTIFY_AUTH_TOKEN` and `defaultUsersList` properties with the values of your auth token and a list of your team's NT usernames to auto assign the fortify report in the [SecureCode Fortify](https://securecode.sample.com/ssc) dashboard.

If this project is the first time your team has created a new service you may need to create a new fortify auth token.
The process is self service and here are the required steps:

1. Join [#cis-securecode](https://sampledigital.slack.com/archives/C0NS72WAG)
2. Send a message in the channel of `I need a token`
3. You will receive a DM with a valid Fortify auth token
4. In your project's fortify.gradle replace `[add_token]` with the token from step 3
5. (Optional) Add the fortify token from step 3 as a global password on your Brewmaster
  1. Login to your team's Brewmaster instance and click Manage Jenkins >> Configure System
  2. In the section `Mask Passwords - Global name/password pairs` click `Add`
  3. Enter `FORTIFY_AUTH_TOKEN` in the name field
  4. Enter the token UUID from step 3 in the password field
  5. All future builds should provide the `FORTIFY_AUTH_TOKEN` password as a gradle system property

## Misc

#### Using Intellij IDEA to create a new project from the blueprint
For a brief visual walkthrough of how to use set up a new project directly from Intellij, see the clips here [Video series](https://confluence.sample.com/display/DEVCOM/viewpage.action?pageId=189114262#UserManual-Videoseries).

#### <a name="fork-sync-explained"></a> Fork-sync explained
Bitbucket has a neat feature where it will automatically propagate new commits to all forks that have the
fork-sync option enabled. This automatic sync goes only in one direction, from the parent to the child repos.

For example, suppose you create a repo `parent` and you fork it to a fork-synced repo `child`. They both have
only one branch, `master`. Then suppose you create a branch `new-branch` in the `parent` repo. You will also see
the `new-branch` appear in the `child` repo. If you push a commit to `parent/new-branch` that commit will also
appear in `child/new-branch`. If you create a PR and merge `parent/new-branch` into `parent/master` then you will
see that change also propagates down to `child/master`. Similarly if you delete `parent/new-branch` you will
no longer see have `child/new-branch`.

If you make a commit directly to `child/master` and someone else makes a different commit to `parent/master` then
fork syncing will no longer be active between the `parent/master` and `child/master` branches. If they come back
into sync again, the fork-sync will resume. (Eg, if `child/master` gets merged into `parent/master`).

See [keeping forks synchronized](https://confluence.atlassian.com/bitbucketserver/keeping-forks-synchronized-776639961.html) for more info.

### Maven Local

The Maven Local repository is helpful for testing library changes locally before publishing. Use the command line
option `-PuseMavenLocal=true` to enable the local Maven repo.  Maven local isn't enabled by default to avoid strange
build errors (someone may have unusual artifacts installed locally or people have sometimes hit issues with Maven
and Gradle not playing nicely together).

See [Publishing to Maven Local](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:install) for how
to publish libraries to the Maven Local repo.


# Code Reviewers
Reviewers of this repo ensure that new changes follow:

* the spirit of the project ([goals](https://confluence.sample.com/pages/viewpage.action?pageId=181347340))
* the outlined [code conventions](#conventions)
* the process laid out in the [contributor docs](#contributor-docs)

Code reviewers may also chime in if they have expertise, experience, or thoughts relating to the changes being made.

## New Reviewers
If you would like to be a reviewer, please reach out to #[cop-blueprint-java](https://sampledigital.slack.com/messages/C744UC1PG/) in Slack.


# <a name="contributor-docs"></a> Contributing
As a community effort, every one of us (including YOU reading this right now!) have the ability to influence the direction, quality, and usefulness of this project!
If you would like to be involved, here are a few suggestions:

* share your observations & ideas: Have an idea or experience an issue using the Blueprint? Call in out in the Slack channel: #[cop-blueprint-java](https://sampledigital.slack.com/messages/C744UC1PG/)
* open a PR: See a typo or outdated config? Is there something in the docs that could be explained more clearly? Create a PR and join the [community](#contributors)!
* add a feature: is there a common piece of functionality that you'd like to be available when creating a new service? Add a new feature branch!


## Git Usage
In order to keep the Git history consistent and easy to read the following guidelines should be followed when contributing.

1. Each PR is only doing one thing
2. Each commit must be self contained (able to be safely reverted)
    * Merges will be done via `-ff-only` merges which means the source branch must be up to date with the target branch.
    * **How**: If your branch is not up to date, rebase your commits onto the target branch before merging. If you have commits that can't be reverted independently, squash those changes into a single commit.
    * **Example**: You commit a change to your branch, then notice the tests broke and so create a second commit to fix the tests. Since the second commit can't be reverted independently of the first commit (without the build breaking), it's recommended you squash the two commits. This can be done with the rebase option or commit the second change using the `--amend` flag.
    * **Why**: This will mean that it is very is easy to revert changes as a single commit will be related to a complete change.
3. Each commit message should start with the JIRA ticket number (if applicable) and be clear, concise, and give the reader
 a good summary of what the commit contains. 
    * For an article on how to create a good commit message see: https://chris.beams.io/posts/git-commit/
    * **Example**: COPBLUE-9999 - Update sample DynamoDB client library
    * **Why**: This should keep the git history very clean so that all maintainers can easily see the state of the branch


## Opening a PR
As a contributor, your PR needs to:

* Show proof of a successful build
    * Follow the directions under [How To Use](#how-to-use) and link to a pipeline showing a successful deploy to test
      (you can also use the [end-to-end script](https://confluence.sample.com/display/DEVCOM/viewpage.action?pageId=182796983) to speed up creation of a test service).
* Follow the [code conventions](#conventions)

## <a name="conventions"></a> Conventions

#### Adding new configuration properties
In the `application.properties` files, only the properties that are actually used (and are important to the functioning
of the service) should be added. There shouldn't be commented out properties or properties set to their default values.
The README of the project or feature branch should document the possible properties and developers that want to enable
or change a feature can bring them into their properties file if needed.

#### Adding new dependencies
To reduce merge conflicts when merging multiple features, new dependencies should continue to follow alphabetical ordering.

#### Move integration code to external library
Any integrations, where possible, should be moved into an external library that has sane defaults. This serves several purposes:

1. Allows you to easily update the implementation and only requiring users to update a dependency
2. Lowers friction for users bring in the integration manually
3. Keeps the blueprint as clean as possible from an end user perspective

There are several examples of this approach and how to setup your integration to auto configure itself within a Springboot context:

* Artemis integration: https://bitbucket.sample.com/projects/ARTEMIS/repos/artemis-integration/browse
* Wingtips integration: https://github.com/sample-Inc/wingtips/tree/master/wingtips-spring-boot
* Cerberus integration: https://github.com/sample-Inc/cerberus-spring-boot-client


## Feature branches

#### Creating a new feature branch
We are happily accepting contributions! There are only a few guidelines that the feature branches need to follow:

* Create a README file - Should contain a description, a changelog like the one below, and any information that might be helpful to others. 
  The filename should contain a reference to your feature (eg, README_HYSTRIX.md).
* Minimize changes to shared classes and use unique names - To maintain consistency between features and avoid merge conflicts
  with other feature branches:
    * use the pattern `{featureName}{className}` - for classes that are part of configuring the feature (e.g., S3Configuration, SpindleExceptionHandler, etc)
    * use the pattern `Example{featureName}{className}` - for classes that can be safely refactored or removed (e.g., ExampleS3Service, ExampleNonBlockingService, ExampleSpindleController, ExampleS3ModelObject, etc)

To add a new feature branch:

1. Fork this project into your team Bitbucket space or your [personal](https://bitbucket.sample.com/profile) Bitbucket space
2. Open your fork in Bitbucket and click the `Create Branch` link in the sidebar.
3. Select `Branch type` as `custom` and `Branch from` as `master`. Name the branch starting with `feature/` (eg `feature/new-feature`)
4. Checkout the newly created branch locally
5. Add your feature code, commit, and push to your own fork
6. When you're ready to create a PR to the main repo, repeat steps 2 and 3 on the main repo and create a PR from your fork's feature branch to the main repo's feature branch

#### Updating existing feature branches
To update an existing feature branch:

1. Fork this project into your team Bitbucket space or your [personal](https://bitbucket.sample.com/profile) Bitbucket space
2. Checkout the feature branch locally
3. Add your changes, commit, and push to your own fork
4. Create a PR from your fork to the main repo's feature branch


## <a name="contributors"></a> Contributors
This project is the result of many contributions from individuals (like yourself!), wanting to make our development experience just a little bit better. Don't be shy about adding yourself (or others) if a contribution has been made!

Big thanks to all these wonderful people (in alphabetical order):

* Amer Zec
* Chandra Gummadi
* Connor Lay
* Dave Napack
* Eric West
* Frank Magistrali
* Ian Warren
* Jeff Hernandez
* Nick Kedev
* Nic Munroe
* Shane Witbeck
* Sujan Adusumilli
* Thomas Good
* Todd Lisonbee
* Vijayakumar Ramar


# Change Log
Details on how to contribute to this changelog see the website
[Keep a change Log.](http://keepachangelog.com/) All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

> Given a version number MAJOR.MINOR.PATCH, increment the:  
> MAJOR version when you make incompatible API changes,  
> MINOR version when you add functionality in a backwards-compatible manner, and  
> PATCH version when you make backwards-compatible bug fixes.

### [0.16.0]
#### Changed
- Update project to SpringBoot 2.1
- Update SpringCloud dependency to Greenwich.RELEASE
- Update Gradle 4.10.2 -> 5.3.1

### [0.15.0]
#### Changed
- Set application context path to default value and moved D/R/V mapping to the controller to better
accommodate sample API filter query patterns.

### [0.14.0]
#### Changed
- Removed old QMA gradle plugin integration
- Collapsed the `qma.gradle` file into `check.gradle`
- Updated default pipeline to use the QT plugin for QMA integration

### [0.13.0]
#### Changed
- Upgraded Gradle to version 4.10.2.
- Upgraded pitest gradle plugin to 1.3.0 to address Gradle 5.0 deprecation.

### [0.12.0]
#### Changed
- Rename application.yaml to pipeline.yaml, move to project root

### [0.11.0]
#### Changed
- Updated API placeholder to openapi v3 with improved documentation

### [0.10.1]
#### Fixed
- Relaxed regex for the Bitbucket project in the init.groovy script

### [0.10.0]
#### Changed
- Simplify log4j config to log everything to the application log file
- Removed console logger from production code
- Added logger to integration tests for better IDE integration

### [0.9.0]
#### Changed
- Update integration test DSL to use the standard job defined in KEYSTONE/gradle-java-microservice

### [0.8.1]
- Added downloadSources and downloadJavadoc for IntelliJ and Eclipse

### [0.8.0]
- Updated Eureka server port to deprecate port 80 ELB

### [0.7.13]
- Update README with instructions for creating Fortify auth token

### [0.7.12]
- Added pipeline support for global tagging standards

### [0.7.11]
- Update README to include Git Usage section

### [0.7.10]
#### Changed
- Moved to `wingtips-spring-boot` dependency which allows us to auto-magically inject and setup the 
`RequestTracingFilter` and configure Wingtips stuff from `application.properties`. This lets us remove tracing stuff
from `BlueprintConfiguration` to keep it as clean as possible, and keeps config stuff in config files. 
- Updated OSS Wingtips lib version to `0.14.1`.
- Updated sample internal Wingtips lib version to `4.1.0.13`.

### [0.7.9]
#### Changed
- Adjusted `ApplicationJsr303AnnotationTroller` to ignore the Spindle library's `PartialUpdate` class, which contains
JSR 303 validation annotations that do not conform to the 
[Backstopper conventions](https://github.com/sample-Inc/backstopper/blob/master/USER_GUIDE.md#jsr303_conventions).
This should prevent `VerifyJsr303ContractTest` from failing during unit tests for projects that pull in the Spindle
libraries.

### [0.7.8]
#### Changed
- Updated Backstopper version to `0.11.3`.
- Excluded `spring-web` and `spring-webmvc` dependencies from Backstopper. This is a no-op currently, but prevents 
potential transitive dependency issues in the future with those core Spring dependencies.

### [0.7.7]
#### Changed
- Incremented version of piTest for DynamoDBLocal. Issue details found at
  https://github.com/szpak/gradle-pitest-plugin/issues/52

### [0.7.6]
#### Changed
- Move the user/code-reviewer/contributor docs from Confluence to project README

### [0.7.5]
#### Changed
- Updated pipeline to use new Jenkins URL schema https://[your_brewmaster].auto.samplecloud.com/
  after all Brewmasters have been migrated out of AWS Tools account

### [0.7.4]
#### Changed
- Update QMA task to work with Gradle 4
- Remove reference to fortify job in keystone

### [0.7.3]
#### Changed
- update fortify configs and remove redundant microjob

### [0.7.2]
#### Added
- exclude configuration classes from QMA
#### Remove
- remove unused unit-test microjob (isn't being triggered. The unit tests are already run as part of the jar/war job)

### [0.7.1]
#### Added
- add option to bootstrap script to include/exclude example code from new project
#### Changed
- rename service controller to prevent deletion when excluding example code

### [0.7.0]
#### Changed
- Moved from the old sample tracing-core libraries for distributed tracing to 
[Wingtips](https://github.com/sample-Inc/wingtips).

### [0.6.1]
#### Changed
- update readme to reflect rename of repo `CEIC/ceic-springboot-blueprint` to `COPBLUE/blueprint-java-springboot`

### [0.6.0]
#### Changed
- Moved to [Backstopper](https://github.com/sample-Inc/backstopper) for error handling. Usage notes can be found in 
the "API Error Handling" section of `README_SERVICE.md`.

### [0.5.0]
#### Changed
- upgraded rest-assured test dependency and associated package name changes

### [0.4.1]
#### Added
- prod section pipeline configuration with new VPC properties
#### Changed
- unpinned `gradle-java-microservice`

### [0.4.0]
#### Changed
- pipeline configuration changes to reflect new VPC
- application properties to reflect new Eureka endpoints

### [0.3.2]
#### Changed
- replaced `stashProjectKey` property with `scmProjectKey`
- replaced `stashRepositoryKey` property with `scmRepositoryKey`
#### Removed
- unused `src/main/resources/application-local.yaml`

### [0.3.1]
#### Changed
- don't ignore failures from checkstyle
- don't ignore failures from findbugs

### [0.3.0]
#### Added
- `org.springframework.boot:spring-boot-starter-log4j2` dependency
- `org.springframework.boot:spring-boot-starter-web` dependency
- exclusions of `spring-boot-starter-logging` and `spring-boot-starter-tomcat` transitive dependencies
#### Changed
- upgraded Spring Cloud from Dalston.M1 to Dalston.SR3
- upgraded Spring Boot from 1.5.0.RELEASE to 1.5.6.RELEASE
- upgraded `disruptor` from 3.3.4 to 3.3.6
#### Removed
- Jackson dependencies in favor of transitive versions from `spring-boot-starter-actuator`
- JUnit dependency in favor of transitive versions from `spring-boot-starter-test`
- Log4J dependencies in favor of `org.springframework.boot:spring-boot-starter-log4j2`

### [0.2.0]
#### Changed
- upgraded Gradle to from version 2.9 to 4.1
- unpinned versions of Jacoco and PMD
#### Removed
- RedundantThrows checkstyle module (removed in Checkstyle 6.2)

### [0.1.4]
#### Fixed
- fix integration tests to verify updated response object

### [0.1.3]
#### Changed
- renamed feature branches to be prefixed with `feature` instead of `example`
- update README to refer to branches as `feature` instead of `example`

### [0.1.2]
#### Fixed
- Controller to return JSON entity by default
- Test for the JSON response

### [0.1.1]
#### Added
- support for local logging

### [0.1.0]
#### Added
- dry run option to init script
#### Changed
- verbiage around creating the bitbucket repo automatically
- unpinned versions of pipeline micro jobs
#### Fixed
- removed restriction of no spaces in team name during application init

### [0.0.3]
#### Changed
- update README with section on usage patterns and fork syncing
- update canary feature file to use simple metric check, allowing for a passing canary analysis
- uncomment `sshKeyName` property in `application.yaml` to allow a passing pipeline (turn out it's a required property)
- reduce the ASG size for TEST deploys to 1 instance (speeds up deployment)

### [0.0.2]
#### Changed
- fix issue causing `iamInstanceProfile` property not to be used, due to incorrect spacing in the `application.yaml`
- update integration tests to accept instance IP if given as system property
- update README with info on how to preview changes through Bitbucket
- update README to highlight making changes to your team or personal fork, and opening a PR into the main repo

### [0.0.1]
#### Changed
- initial cleanup - extracted many included features into example branches (Hystrix, Feign, etc)
