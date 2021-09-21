@Library('corda-shared-build-pipeline-steps@corda5') _

cordaPipeline(
    runIntegrationTests: false,
    runE2eTests: true,
    e2eTestName: 'corda5-token-sdk-e2e-tests',
    nexusAppId: 'com.r3.corda.sdk.token-corda-5',
    dependentJobsNames: [
        "/Corda5/corda5-token-sdk-diamond-demo/${env.BRANCH_NAME.replace('/', '%2F')}"
    ],
    artifactoryRepoRoot: 'corda-os-maven-stable/com/r3/corda/lib/tokens/'
)
