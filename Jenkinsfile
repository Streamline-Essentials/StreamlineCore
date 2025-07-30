pipeline {
    agent any

    environment {
        VERSION = '3.5.5.0-SNAPSHOT' // Set the version of the plugin
        NAME = 'StreamlineCore' // Set the name of the plugin
        COMMIT_HASH = sh(script: 'git rev-parse HEAD', returnStdout: true).trim() // Get the current commit hash
        IS_SNAPSHOT = getIsSnapshotString(version: VERSION) // Check if the version is a snapshot
        FINAL_VERSION = getFinalVersionString(check: VERSION) // Final version without snapshot
    }

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

                // Build
                sh 'gradle build'

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
                archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true, excludes: '**javadoc**, **sources**, **all**, ${NAME}-${VERSION}.jar'
            }
        }

        stage('Publish to Artifactory') {
            steps {
//                 if (IS_SNAPSHOT == 'true') {
//                     jf 'rt u deploy/* gradle-release/${NAME}/${FINAL_VERSION}/ --flat=true --props "version=${FINAL_VERSION};name=${NAME}"'
//                     jf 'rt build-publish'
//                 } else {
//                     jf 'rt u deploy/* gradle-release/${NAME}/${FINAL_VERSION}/ --flat=true --props "version=${FINAL_VERSION};name=${NAME}"'
//                     jf 'rt build-publish'
//                 }
                jf 'rt u deploy/* gradle-release/deployed --flat=true --props "version=${FINAL_VERSION};name=${NAME}"'
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

def isSnapshot(version) {
    release version.endsWith('-SNAPSHOT')
}

def getIsSnapshotString(version) {
    if (isSnapshot(version)) {
        return 'true'
    } else {
        return 'false'
    }
}

def getFinalVersionString(check) {
    if (isSnapshot(check)) {
        return "master-SNAPSHOT"
    } else {
        return check
    }
}