#!/usr/bin/env node

"use strict"; 
// Import necessary modules
var path     = require('path');
var fileUtil = require('./file-util.js');
var logger   = require('./logger.js');

exports.setupDexGuard  = setupDexGuard;
exports.cleanupDexGuard = cleanupDexGuard;

var CHANGES_FILE              = '.dexguard-plugin-changes.json';
var MAIN_ACTIVITY_BACKUP_FILE = '.dexguard-plugin-main-activity.java';
var GRADLE_ROOT_DIRECTIVE     = 'repositories and dependencies';
var GRADLE_ROOT               = 'build-root.gradle';
var CORDOVA_ACTIVITY          = 'CordovaActivity';
var CONFIG_FILE               = 'dexguard-plugin.json';
var BUILD_GRADLE              = 'build.gradle';
var BUILD_EXTRA_GRADLE        = 'build-extras.gradle';
var PACKAGE_NAME_PLACEHOLDER  = 'PACKAGE_NAME_PLACEHOLDER';
var DEXGUARD_PATH_PLACEHOLDER = 'DEXGUARD_PATH_PLACEHOLDER';


function replaceAll(str, find, replace) {
    return str.replace(new RegExp(find, 'g'), replace);
}

function setupDexGuard(projectRoot,
                       androidRoot) 
{
    var pluginDir   = path.dirname(__dirname);
    var configFile  = path.join(projectRoot, CONFIG_FILE);
    var changesFile = path.join(androidRoot, CHANGES_FILE);

    // Keep track of the changes made
    var changes = {
        addedFiles: []
    };

    try {

        var addedFilesCollector = (destPath) => 
            changes.addedFiles.push(path.relative(androidRoot, destPath));
        
        // Read settings from plugin config 
        logger.logInfo("Reading settings in: " + configFile);

        // TODO: log appropriate error messages
        var config = JSON.parse(fileUtil.readFile(configFile));

        var appLevelFiles = config.appLevelFiles
        if (!appLevelFiles) {
            logger.logWarning("No Gradle or DexGuard configuration files are " + 
                              "specified under the option 'appLevelFile' in " + 
                              CONFIG_FILE);
        }

        // If not an array, convert to array with the given argument
        else if (!Array.isArray(appLevelFiles))
        {
            appLevelFiles = [ appLevelFiles ];
        }
        logger.logInfo("The following files will be copied to the app directory: " + 
                       appLevelFiles.join(", "));

        var appDir = path.join(androidRoot, 
                               config.appDirectory ? config.appDirectory : "");
        logger.logInfo("The app directory is set as: " +
                       appDir +
                       ". To change this, modify the option appDirectory in " +
                       CONFIG_FILE);

        if (!config.srcDirectory) {
            logger.error("Option srcDirectory is not specified in " + CONFIG_FILE);
        }
        var javaSrcDir = path.join(appDir, config.srcDirectory);
        logger.logInfo("The Java source directory is set as: " +
                       javaSrcDir +
                       ". To change this, modify the option srcDirectory in " +
                       CONFIG_FILE);

        var customActivityFile  = config.customActivityFile
        var gradleRootExtension = path.join(pluginDir, GRADLE_ROOT)

        var dexguardPath = config.dexguardPath;
        if (!dexguardPath) {
            logger.error("Option dexguardPath is not set in dexguard-plugin.json");
        }
        
        // Copy configuration files to the app-level directory
        logger.logInfo("Copying app-level files...");
        fileUtil.copyFiles(appLevelFiles,
                           projectRoot,
                           appDir,
                           undefined,
                           addedFilesCollector);

        // Updates DEXGUARD_PATH_PLACEHOLDER in app-level build-extras.gradle file.
        let dexguardAbsPath = path.resolve(androidRoot, dexguardPath);

        let appLevelExtraGradleFile    = path.join(appDir, BUILD_EXTRA_GRADLE);
        let gradleExtraContent =
            replaceAll(fileUtil.readFile(appLevelExtraGradleFile), DEXGUARD_PATH_PLACEHOLDER, dexguardAbsPath);

        fileUtil.modifyFile(appLevelExtraGradleFile, (x) => { return gradleExtraContent;});
            
        // Append our own repositories and dependencies to the root-level
        // build.gradle file.
        let rootLevelGradleFile        = path.join(androidRoot, BUILD_GRADLE);
        let gradleRootExtensionContent =
            replaceAll(fileUtil.readFile(gradleRootExtension), DEXGUARD_PATH_PLACEHOLDER, dexguardAbsPath);

        fileUtil.modifyFile(rootLevelGradleFile,
                            fileUtil.appendContentWithDirectives(gradleRootExtensionContent, 
                                                                 GRADLE_ROOT_DIRECTIVE));


        // Copy modified MainActivity java class to the src directory
        if (customActivityFile) {

            if (!config.mainActivity)
            {
                logger.error("When specifying option customActivityFile, you need to " + 
                    "specify the fully qualified name of the main activity of this" + 
                    " application using the option 'mainActivity' as well.");
            }

            let mainActivityPackageElements = config.mainActivity.split(".");
            let mainActivityClassName       = mainActivityPackageElements.pop();
            let mainActivityPackageName     = mainActivityPackageElements.join(".");

            if (!mainActivityClassName || !mainActivityPackageName)
            {
                logger.error("When specifying option customActivityFile, you need to " + 
                    "specify the fully qualified name of the main activity of this" + 
                    " application using the option 'mainActivity' as well.");
            }

            let packageDir = path.join(javaSrcDir, mainActivityPackageElements.join("/"));

            // Copy the custom activity into the src directory, intializing its
            // package in the process.
            fileUtil.copyFile(customActivityFile, 
                              projectRoot, 
                              packageDir,
                              (arg) => arg.replace(PACKAGE_NAME_PLACEHOLDER, 
                                                   mainActivityPackageName),
                              addedFilesCollector);


            // Modify MainActivity such that it inherits from the custom activity
            let mainActivityFile            = path.join(packageDir, mainActivityClassName + ".java");
            let customActivityClass         = customActivityFile.split(".")[0];
            let originalClassDeclaration    = `public class ${mainActivityClassName} extends ${CORDOVA_ACTIVITY}`;
            let replacementClassDeclaration = `public class ${mainActivityClassName} extends ${customActivityClass}`;
            let mainActivityBackupFile      = path.join(androidRoot, MAIN_ACTIVITY_BACKUP_FILE);    

            logger.logInfo("Adapting " + mainActivityFile + " to extend " + customActivityClass + 
                           " instead of " + CORDOVA_ACTIVITY);

            fileUtil.modifyFile(mainActivityFile, (arg) => {
                changes.mainActivityPath = path.relative(androidRoot, mainActivityFile);
                fileUtil.writeFile(mainActivityBackupFile, arg);
                return arg.replace(originalClassDeclaration, 
                                   replacementClassDeclaration);
            })
        }
        // Write changes.
        fileUtil.writeFile(changesFile, JSON.stringify(changes));
    }   
    catch(error)
    {
        // Write changes before repropagating the error.
        fileUtil.writeFile(changesFile, JSON.stringify(changes));
        throw error;
    }

    return;
}


function cleanupDexGuard(androidRoot) {

    var changesFile = path.join(androidRoot, CHANGES_FILE);
    var changes     = {}

    if (fileUtil.exists(changesFile))
    {
        changes = JSON.parse(fileUtil.readFile(changesFile));
        fileUtil.deleteFile(changesFile);
    }

    // Remove all added files
    if (changes.addedFiles)
    {
        for (let addedFile of changes.addedFiles)
        {
            let filePath = path.join(androidRoot, addedFile);
            fileUtil.deleteFile(filePath);
        }
    }

    // Remove additions to root level build.gradle file
    let rootLevelGradleFile = path.join(androidRoot, BUILD_GRADLE);
    fileUtil.modifyFile(rootLevelGradleFile, 
                        fileUtil.removeContentWithDirectives(GRADLE_ROOT_DIRECTIVE));
    

    // Revert MainActivity to its previous state
    var mainActivityBackupFile = path.join(androidRoot, MAIN_ACTIVITY_BACKUP_FILE);
    if (fileUtil.exists(mainActivityBackupFile))
    {
        var mainActivityPath = path.join(androidRoot, changes.mainActivityPath);
        var backupContent    = fileUtil.readFile(mainActivityBackupFile);
        fileUtil.writeFile(mainActivityPath, backupContent);
        fileUtil.deleteFile(mainActivityBackupFile);
    }
}
