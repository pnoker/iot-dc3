module.exports = {
    publicPath: './',
    lintOnSave: true,
    productionSourceMap: false,
    devServer: {
        proxy: {
            '/api': {
                target: 'http://dc3-gateway:8000',
                changeOrigin: true,
                ws: true,
                pathRewrite: {
                    '^/api': '/api'
                }
            }
        }
    }
};
