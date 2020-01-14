#!/bin/env groovy

properties([
    parameters([
        text(name: 'VERSION_NAME'),
    ]),
])

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
            sh './gradlew assembleNightly -PversionName=${params.VERSION_NAME}'
            sh 'ls -la ./app/build/outputs/apk/nightly/'
        }

        stage('upload artifacts') {
            archiveArtifacts artifacts: 'app/build/**/*.apk'
        }
    }
}
