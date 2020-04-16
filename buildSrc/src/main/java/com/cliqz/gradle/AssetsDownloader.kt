package com.cliqz.gradle

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.UUID

/**
 * A simple Gradle Extension to add to simplify tasks configuration and creation
 */
open class AssetsDownloaderExtension {
    var url: String? = null
    var path: String? = ""
    var filename: String? = UUID.randomUUID().toString()
}

/**
 * A configuration plugin that combine the Android App/Library plugin with
 * [de.undercouch.download][Download] plugin to correctly download and unzip an assets archive
 * at build time.
 *
 * Usage:
 * ```
 * ...
 * apply plugin: 'cliqz-assets-downloader'
 *
 * ...
 *
 * assetsDownloader {
 *      url = "https://exmample.org/archives/myarchive.zip"
 *      filename = "archive.zip"
 *      path = "assets_subfolder/nested_folder/nested2"
 * }
 * ```
 *
 * Once the plugin is applied, a configuration must be specified. The *url* field is mandatory,
 * the default *filename* is generated by using [UUID.randomUUID] and *path* is initialized with an
 * empty string.
 */
class AssetsDownloader : Plugin<Project> {
    override fun apply(project: Project) {
        if (!project.pluginManager.hasPlugin("com.android.applications") &&
                !project.pluginManager.hasPlugin("com.android.library")) {
            throw GradleException("No specific android plugin applied")
        }

        val extension = project.extensions.create<AssetsDownloaderExtension>("assetsDownloader")

        project.afterEvaluate {
            if (extension.url == null) {
                throw GradleException("AssetsDownloader configuration: no url was specified")
            }

            if (extension.path == null) {
                throw GradleException("AssetsDownload configuration: no output path was provided")
            }

            // Find the merge.*Assets tasks (but avoid the tests assets)
            val taskNames = this.tasks.names.filter {
                PACKAGE_ASSETS_REGEX.matches(it) && !PACKAGE_TEST_ASSETS_REGEX.matches(it)
            }

            // Configure the merge assets tasks
            taskNames.forEach {
                val relativePath = taskNameToPath(it)
                val flavorName = PACKAGE_ASSETS_REGEX.find(it)!!.groupValues[1]
                val destinationDir = project.buildDir
                        .resolve("intermediates/library_assets/$relativePath/out/${extension.path}")
                        .absoluteFile

                val downloadTaskName = "download${flavorName}Assets"
                val downloadTask = tasks.register<Download>(downloadTaskName)
                downloadTask.configure {
                    val destFile = File(downloadTaskDir, extension.filename!!)
                    src(extension.url)
                    dest(destFile)
                    onlyIfModified(true)
                }

                val unzipTask = tasks.register<Copy>("unzip${flavorName}Assets")
                unzipTask.configure {
                    dependsOn(downloadTask)
                    val dt = tasks.getByName(downloadTaskName) as Download
                    from(zipTree(dt.dest))
                    into(destinationDir)
                }

                val task = tasks.named(it).get()
                task.finalizedBy(unzipTask)
            }
        }
    }

    /**
     * Converts a task name to an useful path, i.e.: mergeFlavorDebugAssets to flavor/debug
     */
    private fun taskNameToPath(taskName: String) = "([A-Z]?[a-z]+)".toRegex()
            .findAll(taskName)
            .drop(1) // Drops "merge"
            .toList() // Sequence has no dropLast
            .dropLast(1) // Drops "Assets"
            .joinToString(separator = File.pathSeparator) { match -> match.value.toLowerCase() }

    companion object {
        val PACKAGE_ASSETS_REGEX = "package(.*)Assets".toRegex()
        val PACKAGE_TEST_ASSETS_REGEX = "package.*TestAssets".toRegex()
    }
}
