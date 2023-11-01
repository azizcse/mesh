#!/usr/bin/env node

"use strict"; 

// Imports
var fs     = require('fs');
var path   = require('path');
var logger = require('./logger.js');

// Exports
exports.writeFile                   = writeFile;
exports.readFile                    = readFile;
exports.copyFile                    = copyFile
exports.copyFiles                   = copyFiles
exports.deleteFile                  = deleteFile;
exports.exists                      = exists;
exports.modifyFile                  = modifyFile;
exports.appendContentWithDirectives = appendContentWithDirectives;
exports.removeContentWithDirectives = removeContentWithDirectives;



function copyFiles(fileNames, 
                   srcDir, 
                   destDir, 
                   extraContentFunction,
                   extraDestPathFunction) {
    if (fileNames) {
        for (let fileName of fileNames) {
            copyFile(fileName, 
                     srcDir, 
                     destDir, 
                     extraContentFunction, 
                     extraDestPathFunction);
        }
    }
}


function copyFile(fileName, 
                  srcDir, 
                  destDir, 
                  extraContentFunction, 
                  extraDestPathFunction) {
    srcDir   = path.join(srcDir, path.dirname(fileName));
    fileName = path.basename(fileName);

    let srcFile  = path.join(srcDir, fileName);
    let destFile = path.join(destDir, fileName);

    // Use read and write functions, since the copy function was only added 
    // in v8.5.0 (2017-09-12)
    logger.logInfo("Copying " + fileName + " from " + srcDir +  " to " + destDir);
    let content = readFile(srcFile);

    if (extraContentFunction)
    {
        content = extraContentFunction(content);
    }

    if (extraDestPathFunction)
    {   
        extraDestPathFunction(destFile);
    }

    writeFile(destFile, content);
}


function readFile(srcFile) {
    return fs.readFileSync(srcFile, 'utf8');
}


function writeFile(destFile, content) {
    fs.writeFileSync(destFile, content); 
}


function deleteFile(filePath) {
    if (exists(filePath)) {
        fs.unlinkSync(filePath);
    }
}


function exists(filePath) {
    return fs.existsSync(filePath);
}


function modifyFile(srcFile, modificationFunction) {
    let content = readFile(srcFile);
    writeFile(srcFile, modificationFunction(content));
}


function appendContentWithDirectives(extraContent, directiveName) {
  return (originalContent) => {
    let startDirective = createStartDirective(directiveName);
    let endDirective   = createEndDirective(directiveName);

    let newContent = [ originalContent.replace(/\n*$/,"\n"),
                       startDirective, 
                       extraContent, 
                       endDirective ].join("\n"); 

    return newContent;
  }
}


function removeContentWithDirectives(directiveName) {
  return (originalContent) => {
    let startDirective = createStartDirective(directiveName);
    let endDirective   = createEndDirective(directiveName);

    let startIndex = originalContent.indexOf(startDirective),
        endIndex   = originalContent.indexOf(endDirective) + endDirective.length;
    let newContent = startIndex == -1 ? 
                        originalContent :
                        originalContent.substring(0, startIndex) +
                        originalContent.substring(endIndex);

    return newContent;
  }
}


function createStartDirective(directiveName) {
    return createDirective(directiveName) + " START";
}


function createEndDirective(directiveName) {
    return createDirective(directiveName) + " END";
}


function createDirective(directiveName) {
  return "// DEXGUARD " + directiveName.toUpperCase();
}







