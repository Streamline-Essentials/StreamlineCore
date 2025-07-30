pipeline {
    agent any

    tools {
        gradle 'Gradle' // Ensure Gradle is installed and configured in Jenkins
        jfrog 'jfrog-cli' // JFrog CLI tool, ensure it's configured in Jenkins
    }

    stages {
        stage('Clone') {
            steps {
                // Checkout code from SCM (Git assumed)
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Chmod all files in the workspace to ensure they are executable
                sh 'find . -type f -exec chmod +x {} \\;'

                // Install gradle dependencies
                jf 'gradle dependencies --refresh-dependencies'

                // Configure Gradle project's repositories
                jf 'gradle-config --repo-resolve libs-release --repo-deploy libs-release-local'

                // Install and publish project
                jf 'gradle clean build artifactoryPublish'

                // Find all .jar files in and only in any "build/libs" directory
                // and move them to the 'deploy' directory
                sh '''
                    mkdir -p deploy
                    find . -type f -name "*.jar" -path "*/build/libs/*" -exec cp {} deploy/ \\;
                '''
            }
        }

        stage('Publish to Jenkins') {
            steps {
                // Publish artifacts to Jenkins
                archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true
            }
        }

        stage('Publish to Artifactory') {
            steps {
                // Upload all files in the 'deploy' directory to the 'gradle-release' Artifactory repository.
                jf 'rt u deploy/ gradle-release/'
                jf 'rt build-publish'
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