#!/usr/bin/env node

"use strict"; 

// Imports
var fs    = require('fs');
var path  = require('path');

// Exports
exports.logWarning = logWarning;
exports.logInfo    = logInfo
exports.error      = error

var DEXGUARD_PREFIX = "[DexGuard-plugin] ";


function logWarning(message)
{
    log("Warning: " + message);
}


function logInfo(message)
{
    log("Info: " + message);
}


function error(message)
{
    throw new Error(DEXGUARD_PREFIX + message);
}


function log(message)
{
    console.log(DEXGUARD_PREFIX + message);
}