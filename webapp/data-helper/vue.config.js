module.exports = {
    publicPath: './',
    outputDir: 'express/public',
    productionSourceMap: false,
    devServer: {
        port: 8081,
        proxy: 'http://localhost:8080'
    }
}