let oldConfig;

window.onload = reload;

function reload() {
    fetch(document.documentURI + "/config").then(r => r.json()).then(config => setValues(config))
        .catch(error => alert("Failed to get config: " + error));
}

function setValues(config) {
    document.getElementById("bundle-version").textContent = config.bundleVersion;

    document.getElementById("releases").checked = config.releasesEnabled;
    document.getElementById("release-version-table").innerHTML = '';
    if (config.releasesEnabled === true) {
        config.releases.forEach(version => {
            document.getElementById("release-version-table")
                .appendChild(addReleaseVersion(version.version, version.enabled));
        });
    } else {
        document.getElementById("release-version-table").textContent = "- not available -";
    }

    document.getElementById("snapshots").checked = config.snapshotsEnabled;
    if (config.snapshotsEnabled === true) {
        document.getElementById("snapshot-version").textContent = config.snapshotVersion;
    } else {
        document.getElementById("snapshot-version").textContent = "- not available -";
    }

    oldConfig = config;
}

function addReleaseVersion(version, enabled) {
    let id = checkboxElementId(version);
    let div = document.createElement("div");

    let checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.id = id;
    checkbox.name = id;
    checkbox.checked = enabled;

    let label = document.createElement("label");
    label.classList.add("checkbox-input-label")
    label.htmlFor = id;
    label.textContent = version + " Release";

    div.appendChild(label);
    div.appendChild(checkbox);

    return div;
}

function checkboxElementId(version) {
    return "version_" + version.replace(".", "_");
}

function applyClick() {
    oldConfig.releases.forEach(function (version) {
        let id = checkboxElementId(version.version);
        version.enabled = document.getElementById(id).checked;
    });

    oldConfig.releasesEnabled = document.getElementById("releases").checked;
    oldConfig.snapshotsEnabled = document.getElementById("snapshots").checked;

    fetch(document.documentURI + "/config", {
        method: 'post',
        body: JSON.stringify(oldConfig)
    }).then(reload);
}