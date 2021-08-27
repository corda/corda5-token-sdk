<p align="center">
    <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda Token SDK

## Reminder

This project is open source under an Apache 2.0 licence. That means you
can submit PRs to fix bugs and add new features if they are not currently
available.

## What is the token SDK?

The token SDK exists to make it easy for CorDapp developers to create
CorDapps which use tokens. Functionality is provided to create token types,
then issue, move and redeem tokens of a particular type.

The tokens SDK comprises three CorDapp JARs:

1. Contracts which contains the base types, states and contracts
2. Workflows which contains flows for issuing, moving and redeeming tokens
   as well as utilities for the above operations.
3. Token Builder which contains builder classes and utility functions for creating token objects
4. Selection that contains both database and in memory token selection of fungible tokens

The token SDK is intended to replace the "finance module" from the core
Corda repository.

For more details behind the token SDK's design, see
[here](design/design.md).

## How to use the SDK?

### Build Tokens SDK against Corda release branch

Often, in order to use the latest `token-sdk` master you will need to build against a specific Corda release branch until 
the required changes make it into a Corda release. At the time of writing tokens `2.0.0-DevPreview-RC04` requires Corda 
`5.0.0-DevPreview-RC04`. 
To build against a corda snapshot you can build this branch with the following commands:

    git clone https://github.com/corda/corda5
    git fetch
    git checkout origin release/os/5.0-DevPreview1
   
You can set the corda5 snapshot version in the `gradle.properties` file. Set the property: `corda_release_version`

Then run a `./gradlew clean install` from the root directory.

### Adding token SDK dependencies to an existing CorDapp

First, add a variable for the tokens release group and the version you 
wish to use and set the corda version that should've been installed locally:

    buildscript {
        ext {
            tokens_release_version = '2.0.0-DevPreview-RC04'
            tokens_release_group = 'com.r3.corda.lib.tokens'
        }
    }

Second, you must add the tokens development artifactory repository to the
list of repositories for your project:

    repositories {
        maven { url 'https://software.r3.com/artifactory/corda-os-maven-stable' }
    }

Now, you can add the `token-sdk` dependencies to the `dependencies` block
in each module of your CorDapp. For contract modules add:

    cordapp "$tokens_release_group:tokens-contracts:$tokens_release_version"

In your workflow `build.gradle` add:

    cordapp "$tokens_release_group:tokens-workflows:$tokens_release_version"
Add the selection module:

    cordapp "$tokens_release_group:tokens-selection:$tokens_release_version"
And add the tokens builder module:

    cordapp "$tokens_release_group:tokens-builder:$tokens_release_version"

### Installing the token SDK binaries

If you wish to build the `token-sdk` from source then do the following to
publish binaries to your local maven repository:

    git clone http://github.com/corda/corda5-token-sdk
    cd token-sdk
    ./gradlew clean install

## Where to go next?

[Official documentation](https://engineering.r3.com/product-areas/corda-platform/api-and-modularity/docs/)
_Note: this currently points to internal documentation and should be updated to point to public documentation when made available._

[Introduction to token SDK](docs/OVERVIEW.md)

[Most common tasks](docs/IWantTo.md)

[Simple delivery versus payment tutorial](docs/DvPTutorial.md)

## Other useful links

[Token SDK Design](design/design.md)

[Experimental in memory token selection](docs/InMemoryTokenSelection.md)

[Changelog](CHANGELOG.md)

[Contributing](CONTRIBUTING.md)

[Contributors](CONTRIBUTORS.md)
