#!/bin/env groovy

node('master'){
    def imageName = 'cliqz-daisy'
    stage('Checkout'){
        checkout scm
    }
    stage('Build docker image') {
        docker.build(imageName, '--build-arg UID=`id -u` --build-arg GID=`id -g` .')
    }

    docker.image(imageName).inside() {
        stage('gradle assemble') {
            sh './gradlew assembleNightly -PversionName=0.0.1'
            sh 'ls -la ./app/build/outputs/apk/nightly/'
        }

        stage('upload artifacts') {
            archiveArtifacts artifacts: 'app/build/**/*.apk'
        }
    }
}
