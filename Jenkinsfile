pipeline {
    agent any

    environment {
        DONT_COLLECT = 'FOO'
    }

    tools {
        gradle 'Gradle' // Ensure 'Gradle' is configured in Jenkins Global Tool Configuration
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM (Git assumed)
                checkout scm
            }
        }

        stage('Artifactory Configuration') {
            steps {
                rtServer (
                    id: 'artifactory-server',
                    url: 'https://repo.drak.gg/artifactory',
                    credentialsId: 'jfrog-creds'
                )

                rtGradleDeployer (
                    id: 'GRADLE_DEPLOYER',
                    serverId: 'artifactory-server',
                    repo: 'gradle-release',
                    excludePatterns: ['*.war']
                )

                rtGradleResolver (
                    id: 'GRADLE_RESOLVER',
                    serverId: 'artifactory-server',
                    repo: 'gradle-release-local'
                )
            }
        }

        stage('Build') {
            steps {
                // Chmod all files in the workspace to ensure they are executable
                sh 'find . -type f -exec chmod +x {} \\;'
                // Use Gradle wrapper for consistency
                sh './gradlew StreamlineCore-BAPI:build'
            }
        }

        stage('Publish Artifacts') {
            when {
                branch pattern: 'main|master', comparator: 'REGEXP'
            }
            steps {
                // Archive build artifacts
                archiveArtifacts artifacts: '**/deploy/*.jar', allowEmptyArchive: true
            }
        }

        stage('Config Build Info') {
            steps {
                rtBuildInfo (
                    captureEnv: true,
                    includeEnvPatterns: ['*'],
                    excludeEnvPatterns: ['DONT_COLLECT'] // Exact match for DONT_COLLECT
                )
            }
        }

        stage('Exec Gradle') {
            steps {
                rtGradleRun (
                    usesPlugin: true, // Artifactory plugin defined in build.gradle
                    useWrapper: true, // Use Gradle wrapper
                    tasks: 'clean artifactoryPublish',
                    deployerId: 'GRADLE_DEPLOYER',
                    resolverId: 'GRADLE_RESOLVER'
                )
            }
        }

        stage('Publish Build Info') {
            steps {
                rtPublishBuildInfo (
                    serverId: 'artifactory-server'
                )
            }
        }
    }

    post {
        always {
            // Clean workspace, but preserve artifacts/logs on failure for debugging
            cleanWs cleanWhenFailure: false
        }
        success {
            echo 'Build and tests completed successfully!'
        }
        failure {
            echo 'Build or tests failed! Check logs and artifacts for details.'
        }
    }
}