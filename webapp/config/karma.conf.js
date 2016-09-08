module.exports = function(config) {
    config.set({
        browsers: ['PhantomJS'],


        phantomjsLauncher: {
            // Have phantomjs exit if a ResourceError is encountered (useful if karma exits without killing phantom)
            exitOnResourceError: true
        }
    })
}