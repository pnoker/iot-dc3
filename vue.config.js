module.exports = {
    publicPath: './',
    lintOnSave: true,
    productionSourceMap: false,
    devServer: {
        proxy: {
            '/api': {
                target: `http://${process.env.APP_API_HOST}:${process.env.APP_API_PORT}`,
                changeOrigin: true,
                ws: true,
                pathRewrite: {
                    '^/api': '/api'
                }
            }
        }
    }
};
