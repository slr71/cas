const cas = require('../../cas.js');
const assert = require('assert');
const fs = require('fs');
const os = require("os");

(async () => {
    let kid = (Math.random() + 1).toString(36).substring(4);
    const tempDir = os.tmpdir();
    console.log(`Generated kid ${kid}`)
    let configFilePath = tempDir + "/keystore.jwks";
    let config = JSON.parse(fs.readFileSync(configFilePath));
    await cas.doGet("https://localhost:8443/cas/oidc/jwks",
        res => {
            assert(res.status === 200)
            assert(res.data.keys[0]["kid"] !== kid)
        },
        error => {
            throw error;
        })

    config.keys[0]["kid"] = kid;
    console.log(`Updated configuration:\n${JSON.stringify(config)}`);
    await fs.writeFileSync(configFilePath, JSON.stringify(config));
    await cas.sleep(1000)
    await cas.doGet("https://localhost:8443/cas/oidc/jwks",
        res => {
            assert(res.status === 200)
            assert(res.data.keys[0]["kid"] === kid)
        },
        error => {
            throw error;
        })

})();
