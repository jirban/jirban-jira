var webpack = require('webpack');
var webpackMerge = require('webpack-merge');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var commonConfig = require('./webpack.common.js');
var helpers = require('./helpers');

const ENV = process.env.NODE_ENV = process.env.ENV = 'production';

module.exports = webpackMerge(commonConfig, {
  devtool: 'source-map',

  /**
   * Note that publicPath is not set here at build time since we don't know exactly where the war will be deployed. e.g. in the
   * SDK environment it will be prefixed by '/jira' so that the public path is:
   *
   *    /jira/download/resources/org.jirban.jirban-jira/webapp/
   *
   * On the jboss production server there is no '/jira' prefix so the public path is:
   *
   *    /download/resources/org.jirban.jirban-jira/webapp/
   *
   * boot.js sets the global __webpack_public_path__ which then calculates the path dynamically.
   *
   * Other people might deploy jira in other places.
   */
  output: {
    path: helpers.root('..', 'target', 'classes', 'webapp'),
    filename: '[name].[hash].js',
    chunkFilename: '[id].[hash].chunk.js'
  },

  htmlLoader: {
    minimize: false // workaround for ng2
  },

  plugins: [
    new webpack.NoErrorsPlugin(),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.UglifyJsPlugin({ // https://github.com/angular/angular/issues/10618
      mangle: {
        keep_fnames: true
      }
    }),
    new ExtractTextPlugin('[name].[hash].css'),
    new webpack.DefinePlugin({
      'process.env': {
        'ENV': JSON.stringify(ENV)
      }
    })
  ]
});
