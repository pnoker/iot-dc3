#!/usr/bin/env node

const {semver, error} = require('@vue/cli-shared-utils');
const requiredVersion = require('@vue/cli-service/package.json').engines.node;

if (!semver.satisfies(process.version, requiredVersion, {includePrerelease: true})) {
    error(
        `You are using Node ${process.version}, but vue-cli-service ` +
        `requires Node ${requiredVersion}.\nPlease upgrade your Node version.`
    );
    process.exit(1)
}

const Service = require('@vue/cli-service/lib/Service');
const service = new Service(process.env.VUE_CLI_CONTEXT || process.cwd());

const rawArgv = process.argv.slice(2);
const args = require('minimist')(rawArgv, {
    boolean: [
        // build
        'modern',
        'report',
        'report-json',
        'inline-vue',
        'watch',
        // serve
        'open',
        'copy',
        'https',
        // inspect
        'verbose'
    ]
});

const dotenv = require("dotenv");
const dotenvExpand = require("dotenv-expand");
const path = require("path");
const command = args._[0];
const env = args.env || "dev";
loadEnv(env);

service.run(command, args, rawArgv).catch(err => {
    error(err);
    process.exit(1)
});

function loadEnv(env) {
    try {
        const envOptions = ["dev", "test", "mock", "prod"];
        if (!envOptions.includes(env)) {
            throw new Error(
                `env: ${env} is invalid, options: ${JSON.stringify(envOptions)}`
            );
        }
        const envPath = path.resolve(process.cwd(), `config/environments/.env.${env}`);
        const localPath = `${envPath}.local`;
        // 加载.env.local文件
        const localEnvConfig = dotenv.config({
            path: localPath,
            debug: process.env.DEBUG,
        });
        dotenvExpand.expand(localEnvConfig);
        // 加载env文件
        const envConfig = dotenv.config({path: envPath, debug: process.env.DEBUG});
        dotenvExpand.expand(envConfig);
    } catch (err) {
        // 忽略文件不存在错误
        if (err.toString().indexOf("ENOENT") < 0) {
            error(err);
            process.exit(1)
        }
    }
}
