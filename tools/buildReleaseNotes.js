const http = require('https');
const fs = require('fs');

const options = {
    host: 'api.github.com',
    path: '/repos/smarthomej/addons/pulls?page=1&per_page=100&state=closed',
    headers: {'User-Agent': 'SmartHomeJ / 1.0.0'}
};

const releaseTag = process.argv[2];

if (releaseTag == null || !releaseTag.match(/\d+\.\d+\.\d+/)) {
    console.error("Please provide release tag as argument in x.y.z format.");
    return;
}

const request = http.request(options, function (res) {
    let rawData = '';
    res.on('data', function (chunk) {
        rawData += chunk;
    });
    res.on('end', function () {
        try {
            let fullJson = JSON.parse(rawData);
            let releasePr = [];
            fullJson.forEach(pr => {
                if (pr.merged_at != null && pr.milestone != null && pr.milestone.title === releaseTag) {
                    console.log(pr.url);
                    let module = pr.title.match(/\[(.*)\]/)[1];
                    releasePr.push({
                        'module': module,
                        'label': pr.labels.map(label => label.name).filter(item => item !== 'communityapproved'),
                        'title': pr.title.match(/\[.*\]\s(.*)/)[1],
                        'issue': parseInt(pr.url.match(/\/(\d+)$/)[1]),
                        'url': pr.html_url
                    });

                }
            });
            releasePr.sort(function (a, b) {
                // sort-order: module, issue
                if (a.module === b.module) {
                    return a.issue < b.issue ? -1 : 1;
                } else {
                    return a.module < b.module ? -1 : 1;
                }
            });

            // output the table
            let output = '# SmartHome/J Release ' + releaseTag + '\n\n';
            output += 'This is the latest release of the SmartHome/J addons.\n';
            output += 'Please see below for a list of all changes since the last release.\n\n';
            output += '## Changelog\n\n';

            // infrastructure
            output += '### General/Infrastructure\n\n';
            output += '| Type | Issue | Description |\n'
            output += '|---|:---:|---|\n';
            releasePr.forEach(pr => {
                if (pr.module === "infrastructure") {
                    output += '|';
                    pr.label.forEach(label => output += label + " ");
                    output += '|[#' + pr.issue + '](' + pr.url + ')|' + pr.title + '|\n';
                }
            });

            // modules
            output += '\n### Individual Modules\n\n';
            output += '| Module | Type | Issue | Description |\n'
            output += '|---|---|:---:|---|\n';
            let lastModule = ''
            releasePr.forEach(pr => {
                if (pr.module !== "infrastructure") {
                    output += '|' + (lastModule !== pr.module ? pr.module : ' ') + '|';
                    pr.label.forEach(label => output += label + " ");
                    output += '|[#' + pr.issue + '](' + pr.url + ')|' + pr.title + '|\n';
                    lastModule = pr.module;
                }
            });

            // check if target directory exists (using target prevents locally generated release notes from being checked in)
            if (!fs.existsSync('target')) {
                fs.mkdir('target', (err) => {
                    if (err) {
                        console.error(err)
                    }
                });
            }

            fs.writeFile('target/releaseNotes-' + releaseTag + '.md', output, (err) => {
                if (err) {
                    console.error(err);
                }
            });
        } catch (error) {
            console.error(error.message);
        }
    });
});

request.on('error', function (e) {
    console.error(e.message);
});

request.end();