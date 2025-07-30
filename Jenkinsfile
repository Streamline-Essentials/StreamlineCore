pipeline {
    agent any

    environment {
        DONT_COLLECT = 'FOO'
    }

    tools {
        // Specify the Gradle version configured in Jenkins
        gradle 'Gradle'
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM (e.g., Git)
                checkout scm
            }
        }

        stage ('Artifactory Configuration') {
            steps {
                rtServer (
                    id: "artifactory-server",
                    url: "https://repo.drak.gg/artifactory",
                    credentialsId: "jfrog-creds",
                )

                rtGradleDeployer (
                    id: "GRADLE_DEPLOYER",
                    serverId: "artifactory-server",
                    repo: "gradle-release",
                    excludePatterns: ["*.war"],
                )

                rtGradleResolver (
                    id: "GRADLE_RESOLVER",
                    serverId: "artifactory-server",
                    repo: "gradle-release-local",
                )
            }
        }

        stage('Build') {
            steps {
                // Run Gradle build
                sh 'gradle StreamlineCore-BAPI:build'
            }
        }

        stage('Publish Artifacts') {
            when {
                or {
                    branch 'main'
                    branch 'master'
                }
            }

            steps {
                // Archive build artifacts
                archiveArtifacts artifacts: 'deploy/*.jar', allowEmptyArchive: true
            }
        }

        stage ('Config Build Info') {
            steps {
                rtBuildInfo (
                    captureEnv: true,
                    includeEnvPatterns: ["*"],
                    excludeEnvPatterns: ["DONT_COLLECT*"]
                )
            }
        }

        stage ('Exec Gradle') {
            steps {
                rtGradleRun (
                    usesPlugin: true, // Artifactory plugin already defined in build script
                    useWrapper: true,
                    tool: "Gradle", // Tool name from Jenkins configuration
//                     rootDir: "gradle-examples/gradle-example-publish/",
                    tasks: 'clean artifactoryPublish',
                    deployerId: "GRADLE_DEPLOYER",
                    resolverId: "GRADLE_RESOLVER"
                )
            }
        }

        stage ('Publish build info') {
            steps {
                rtPublishBuildInfo (
                    serverId: "artifactory-server"
                )
            }
        }
    }

    post {
        always {
            // Clean workspace after build
            cleanWs()
        }

        success {
            // Notify on success
            echo 'Build and tests completed successfully!'
        }

        failure {
            // Notify on failure
            echo 'Build or tests failed!'
        }
    }
}