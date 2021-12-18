const fs = require('fs');

const bundles = ['automation.javarule', 'binding.amazonechocontrol', 'binding.androiddebugbridge',
    'binding.deconz', 'binding.dmx', 'binding.http', 'binding.knx', 'binding.mail',
    'binding.notificationsforfiretv', 'binding.onewire', 'binding.snmp', 'binding.tcpudp', 'binding.telenot',
    'binding.tr064', 'binding.viessmann', 'persistence.influxdb', 'transform.basicprofiles', 'transform.chain', 'transform.format',
    'transform.math'];

const releaseTag = process.argv[2];

if (releaseTag == null || !releaseTag.match(/\d+\.\d+\.\d+/)) {
    console.error("Please provide release tag as argument in x.y.z format.");
    return;
}

let addons = [];

bundles.forEach(bundle => {
    let fullBundleName = 'org.smarthomej.' + bundle;
    let addon = {};
    addon['id'] = 'org-smarthome-' + bundle.replaceAll('\.', '-');
    addon['type'] = bundle.substr(0, bundle.indexOf('.'));
    addon['version'] = releaseTag;
    addon['author'] = 'SmartHome/J';
    addon['maturity'] = 'stable';
    addon['content_type'] = 'application/vnd.openhab.feature;type=karfile';
    addon['link'] = 'https://docs.smarthomej.org/' + releaseTag + '/' + fullBundleName + '.html';
    addon['url'] = 'https://repo1.maven.org/maven2/org/smarthomej/addons/bundles/' + fullBundleName + '/' + releaseTag + '/' + fullBundleName + '-' + releaseTag + '.kar';

    let readmePath = 'bundles/'+fullBundleName+'/README.md';

    let readme = [];
    try {
        readme = fs.readFileSync(readmePath, 'utf8').split(/\r?\n/);
    } catch (err) {
        console.error(err)
    }

    if (readme.length > 1) {
        addon['title'] = 'SmartHome/J ' + readme[0].substr(2);
    }

    let line = 2;
    addon['description'] = readme[2];

    while (readme[line++].length > 0) {
        addon['description'] += ' ' + readme[line];
    }

    addons.push(addon);
});

addons.sort((a, b) => a.id < b.id);

let output = JSON.stringify(addons, null, 2);

// check if target directory exists (using target prevents locally generated addons.json from being checked in)

if (!fs.existsSync('target')) {
    fs.mkdir('target', (err) => {
        if (err) {
            console.error(err)
        }
    });
}

fs.writeFile('target/addons.json', output, (err) => {
    if (err) {
        console.error(err);
    }
});
