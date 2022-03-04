module.exports = {
    publicPath: './',
    lintOnSave: true,
    productionSourceMap: false,
    devServer: {
        proxy: {
            '/api': {
                target: process.env.VUE_APP_API_URL,
                changeOrigin: true,
                ws: true,
                pathRewrite: {
                    '^/api': '/api'
                }
            }
        }
    }
};
