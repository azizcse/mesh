#!/usr/bin/env node
"use strict"; 

var PLATFORM_ANDROID = 'platforms/android';

var path = require('path');
var util = require('./dexguard-functions.js');

module.exports = function(ctx) {
    // make sure android platform is part of build
    if (ctx.opts.cordova.platforms.indexOf('android') < 0) {
        return;
    }

    var projectRoot = ctx.opts.projectRoot;
    var androidRoot = path.join(projectRoot, PLATFORM_ANDROID);

    util.cleanupDexGuard(androidRoot);
}