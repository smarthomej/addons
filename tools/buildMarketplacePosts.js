const http = require('https');
const fs = require('fs');

const options = {
    host: 'api.github.com',
    path: '/repos/smarthomej/addons/pulls?page=1&per_page=100&state=closed',
    headers: {'User-Agent': 'SmartHomeJ / 1.0.0'}
};

const bundles = ['automation.javarule', 'binding.amazonechocontrol', 'binding.androiddebugbridge',
    'binding.deconz', 'binding.dmx', 'binding.http', 'binding.knx', 'binding.mail', 'binding.mpd',
    'binding.notificationsforfiretv', 'binding.onewire', 'binding.snmp', 'binding.tcpudp', 'binding.telenot',
    'binding.tr064', 'persistence.influxdb', 'transform.basicprofiles', 'transform.chain', 'transform.format',
    'transform.math'];

const releaseTag = process.argv[2];

if (releaseTag == null || !releaseTag.match(/\d+\.\d+\.\d+/)) {
    console.error("Please provide release tag as argument in x.y.z format.");
    return;
}

const microRelease = releaseTag.substr(releaseTag.lastIndexOf(".") + 1, releaseTag.length);

const request = http.request(options, function (res) {
    let rawData = '';
    res.on('data', function (chunk) {
        rawData += chunk;
    });
    res.on('end', function () {
        bundles.forEach(bundle => {
            let bundleName = bundle.substr(bundle.lastIndexOf(".") + 1, bundle.length);

            try {
                let fullJson = JSON.parse(rawData);
                let releasePr = [];
                fullJson.forEach(pr => {
                    if (pr.merged_at != null && pr.title != null && pr.milestone != null &&
                        (pr.title.startsWith("[" + bundleName + "]") || pr.title.startsWith("[" + bundle + "]"))) {
                        let milestone = pr.milestone.title.replace(/3\.1\./, "3.2.");
                        if (["3.2.0", "3.2.1", "3.2.2"].includes(milestone)) {
                            milestone = "3.2.3";
                        }

                        let milestoneMicro = milestone.substr(milestone.lastIndexOf(".")+1, milestone.length);

                        if (milestoneMicro <= microRelease) {
                            releasePr.push({
                                'milestone': milestone,
                                'label': pr.labels.map(label => label.name),
                                'title': pr.title.match(/\[.*\]\s(.*)/)[1]
                            });
                        }
                    }
                });
                releasePr.sort(function (a, b) {
                    // sort-order: module, issue
                    if (a.milestone === b.milestone) {
                        return 1;
                    } else {
                        return a.milestone < b.milestone ? 1 : -1;
                    }
                });

                let fullBundleName = "org.smarthomej." + bundle;

                // output the table
                let output = "[Documentation](https://docs.smarthomej.org/" + releaseTag + "/" + fullBundleName + ".html)\n\n"

                output += '## Changelog\n';

                let lastMilestone = '';
                releasePr.forEach(pr => {
                    if (lastMilestone !== pr.milestone) {
                        output += "\n### Version " + pr.milestone + "\n\n";
                        lastMilestone = pr.milestone;
                    }
                    output += "* " + pr.label.join(" & ") + ": " + pr.title + "\n";
                });

                // documentation and link
                output += "\n## Resources\n\n";

                output += "[" + fullBundleName + "-" + releaseTag + ".kar](https://repo1.maven.org/maven2/org/smarthomej/addons/bundles/"
                    + fullBundleName + "/" + releaseTag + "/" + fullBundleName + "-" + releaseTag + ".kar)\n";

                // check if target directory exists (using target prevents locally generated release notes from being checked in)
                if (!fs.existsSync('target')) {
                    fs.mkdir('target', (err) => {
                        if (err) {
                            console.error(err)
                        }
                    });
                }

                fs.writeFile('target/marketplace-' + bundleName + '.md', output, (err) => {
                    if (err) {
                        console.error(err);
                    }
                });
            } catch (error) {
                console.error(error.message);
            }
        });
    });
});

request.on('error', function (e) {
    console.error(e.message);
});

request.end();

