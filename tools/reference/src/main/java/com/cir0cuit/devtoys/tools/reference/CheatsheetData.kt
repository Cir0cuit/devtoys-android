package com.cir0cuit.devtoys.tools.reference

data class CheatCommand(val title: String, val command: String, val description: String)
data class CheatCategory(val name: String, val commands: List<CheatCommand>)
data class Cheatsheet(val title: String, val categories: List<CheatCategory>)

val CHEATSHEETS = listOf(
    Cheatsheet("Git", listOf(
        CheatCategory("Basics", listOf(
            CheatCommand("Clone", "git clone <url>", "Clone an existing remote repository into a new local directory."),
            CheatCommand("Initialize", "git init", "Create an empty Git repository or reinitialize an existing one."),
            CheatCommand("Add changes", "git add .", "Stage all modified and new files in the current directory for the next commit."),
            CheatCommand("Commit", "git commit -m \"Message\"", "Record changes to the repository with a descriptive message."),
            CheatCommand("Status", "git status", "Show the working tree status (staged, unstaged, and untracked files)."),
            CheatCommand("Log", "git log --oneline", "Show the commit history in a compact, single-line format."),
            CheatCommand("Diff", "git diff", "Show unstaged changes (working tree vs. index). Use 'git diff --staged' for staged changes.")
        )),
        CheatCategory("Branching & merging", listOf(
            CheatCommand("Create branch", "git checkout -b <name>", "Create a new branch and immediately switch to it."),
            CheatCommand("Switch branch", "git checkout <name>", "Switch the working tree to the specified branch."),
            CheatCommand("List branches", "git branch -a", "List all local and remote-tracking branches."),
            CheatCommand("Merge", "git merge <branch>", "Merge the specified branch's history into the current branch."),
            CheatCommand("Delete branch", "git branch -d <name>", "Delete the specified local branch safely (prevents deleting unmerged changes).")
        )),
        CheatCategory("Remote", listOf(
            CheatCommand("Add remote", "git remote add origin <url>", "Add a new remote repository to track."),
            CheatCommand("Fetch", "git fetch", "Download objects and refs from another repository without merging them."),
            CheatCommand("Pull", "git pull origin <branch>", "Fetch from and integrate with another repository or a local branch."),
            CheatCommand("Push", "git push origin <branch>", "Update remote refs along with associated objects.")
        )),
        CheatCategory("Undoing", listOf(
            CheatCommand("Soft reset", "git reset --soft HEAD~1", "Undo the last commit but keep the changes staged in your working directory."),
            CheatCommand("Hard reset", "git reset --hard HEAD~1", "Completely discard the last commit and all uncommitted changes."),
            CheatCommand("Revert", "git revert <commit>", "Create a new commit that undoes the changes from a previous commit safely."),
            CheatCommand("Discard file", "git restore <file>", "Discard local modifications to a specific file in the working directory.")
        )),
        CheatCategory("Stashing", listOf(
            CheatCommand("Stash changes", "git stash", "Temporarily save uncommitted changes and revert the working directory to match the HEAD commit."),
            CheatCommand("Apply stash", "git stash pop", "Apply the most recently stashed changes and remove them from the stash list."),
            CheatCommand("List stashes", "git stash list", "Show all currently stashed changes.")
        )),
        CheatCategory("History & rewriting", listOf(
            CheatCommand("Amend last commit", "git commit --amend --no-edit", "Add the staged changes to the last commit and keep its message."),
            CheatCommand("Interactive rebase", "git rebase -i HEAD~<n>", "Reorder, squash, reword or drop the last n commits."),
            CheatCommand("Rebase onto branch", "git rebase <branch>", "Replay the current branch's commits on top of another branch."),
            CheatCommand("Cherry-pick", "git cherry-pick <commit>", "Apply the changes from one existing commit onto the current branch."),
            CheatCommand("Reflog", "git reflog", "Show where HEAD has pointed recently. Use it to recover lost commits."),
            CheatCommand("Blame", "git blame <file>", "Show which commit and author last changed each line of a file.")
        )),
        CheatCategory("Bisect", listOf(
            CheatCommand("Start bisect", "git bisect start <bad> <good>", "Binary-search the history between a bad and a good commit."),
            CheatCommand("Mark good / bad", "git bisect good", "Mark the checked-out commit as good. Use 'git bisect bad' for a bad one."),
            CheatCommand("Automate bisect", "git bisect run <script>", "Run a script on each step; exit code 0 means good."),
            CheatCommand("End bisect", "git bisect reset", "Stop bisecting and return to the original branch.")
        )),
        CheatCategory("Tags & worktrees", listOf(
            CheatCommand("Annotated tag", "git tag -a <tag> -m \"Message\"", "Create an annotated tag on the current commit."),
            CheatCommand("Push tags", "git push origin --tags", "Push all local tags to the remote."),
            CheatCommand("Delete remote tag", "git push origin --delete <tag>", "Remove a tag from the remote repository."),
            CheatCommand("Add worktree", "git worktree add ../<dir> <branch>", "Check out a branch into a second working directory."),
            CheatCommand("List worktrees", "git worktree list", "Show all working directories linked to this repository."),
            CheatCommand("Remove worktree", "git worktree remove <dir>", "Delete a linked working directory.")
        )),
    )),
    Cheatsheet("Linux / Bash", listOf(
        CheatCategory("File system", listOf(
            CheatCommand("List files", "ls -lah", "List files and directories in long format, including hidden files and human-readable sizes."),
            CheatCommand("Change directory", "cd /path/to/dir", "Change the current working directory."),
            CheatCommand("Print working directory", "pwd", "Print the absolute path of the current working directory."),
            CheatCommand("Make directory", "mkdir -p dir/sub", "Create directories and any necessary parent directories."),
            CheatCommand("Remove directory", "rm -rf <dir>", "Forcefully remove a directory and its contents recursively. Use with caution."),
            CheatCommand("Copy", "cp -r <src> <dest>", "Copy files and directories recursively."),
            CheatCommand("Move / rename", "mv <src> <dest>", "Move or rename files and directories."),
            CheatCommand("Find file", "find . -name \"*.txt\"", "Search for files by name within the current directory hierarchy.")
        )),
        CheatCategory("Process management", listOf(
            CheatCommand("List processes", "ps aux", "Display a snapshot of all currently running processes."),
            CheatCommand("Interactive monitor", "top", "Display real-time system summary and running processes (use htop if available)."),
            CheatCommand("Kill by PID", "kill -9 <pid>", "Forcefully terminate a process using its Process ID."),
            CheatCommand("Kill by name", "killall <name>", "Kill all processes matching a specific name."),
            CheatCommand("Background job", "<command> &", "Run a command in the background, freeing up the terminal.")
        )),
        CheatCategory("Networking", listOf(
            CheatCommand("Ping", "ping <host>", "Send ICMP ECHO_REQUEST to network hosts to check connectivity."),
            CheatCommand("Open ports", "netstat -tuln", "Show all listening TCP and UDP ports and their corresponding services."),
            CheatCommand("Download file", "wget <url>", "Download a file from the internet via HTTP, HTTPS, or FTP."),
            CheatCommand("HTTP request", "curl -X GET <url>", "Transfer data from or to a server using HTTP.")
        )),
        CheatCategory("System & archives", listOf(
            CheatCommand("Disk space", "df -h", "Show file system disk space usage in human-readable format."),
            CheatCommand("Memory usage", "free -m", "Display amount of free and used memory in the system in megabytes."),
            CheatCommand("Create tarball", "tar -czvf archive.tar.gz /dir", "Compress a directory into a gzip-compressed tar archive."),
            CheatCommand("Extract tarball", "tar -xzvf archive.tar.gz", "Extract a gzip-compressed tar archive into the current directory.")
        ))
    )),
    Cheatsheet("SSH", listOf(
        CheatCategory("Keys", listOf(
            CheatCommand("Generate key", "ssh-keygen -t ed25519 -C \"<email>\"", "Create a new Ed25519 key pair in ~/.ssh."),
            CheatCommand("Install public key", "ssh-copy-id <user>@<host>", "Append your public key to the server's authorized_keys."),
            CheatCommand("Add key to agent", "ssh-add ~/.ssh/id_ed25519", "Load a private key into ssh-agent so you type the passphrase once."),
            CheatCommand("Key fingerprint", "ssh-keygen -lf ~/.ssh/id_ed25519.pub", "Show the fingerprint of a public key."),
            CheatCommand("Forget host key", "ssh-keygen -R <host>", "Remove a host from known_hosts after its key changed.")
        )),
        CheatCategory("Connecting", listOf(
            CheatCommand("Connect", "ssh <user>@<host>", "Open a shell on a remote host."),
            CheatCommand("Custom port", "ssh -p <port> <user>@<host>", "Connect to an SSH server that is not on port 22."),
            CheatCommand("Specific key", "ssh -i <keyfile> <user>@<host>", "Authenticate with a given private key file."),
            CheatCommand("Jump host", "ssh -J <jump> <user>@<host>", "Connect through a bastion host."),
            CheatCommand("Run one command", "ssh <user>@<host> '<command>'", "Run a command remotely and print its output.")
        )),
        CheatCategory("Tunnels", listOf(
            CheatCommand("Local forward", "ssh -L 8080:localhost:80 <user>@<host>", "Local port 8080 reaches port 80 on the server."),
            CheatCommand("Remote forward", "ssh -R 9000:localhost:3000 <user>@<host>", "Server port 9000 reaches port 3000 on your machine."),
            CheatCommand("SOCKS proxy", "ssh -D 1080 -N <user>@<host>", "Start a SOCKS5 proxy on local port 1080 through the server."),
            CheatCommand("Tunnel only", "ssh -fN -L 5432:localhost:5432 <user>@<host>", "Keep a tunnel open in the background without a shell.")
        )),
        CheatCategory("Copying files", listOf(
            CheatCommand("Upload with scp", "scp <file> <user>@<host>:<path>", "Copy a local file to the remote host."),
            CheatCommand("Download with scp", "scp -r <user>@<host>:<path> .", "Copy a remote file or directory to the current directory."),
            CheatCommand("Sync with rsync", "rsync -avz --progress <src>/ <user>@<host>:<dest>/", "Copy only changed files, compressed, with progress."),
            CheatCommand("Mirror with rsync", "rsync -avz --delete <src>/ <user>@<host>:<dest>/", "Make the destination identical, deleting extra files."),
            CheatCommand("Dry run", "rsync -avzn <src>/ <user>@<host>:<dest>/", "Show what rsync would transfer without changing anything.")
        )),
        CheatCategory("Config file", listOf(
            CheatCommand("Host entry", "Host <alias>\n    HostName <host>\n    User <user>\n    Port 22\n    IdentityFile ~/.ssh/id_ed25519", "Add to ~/.ssh/config, then connect with 'ssh <alias>'."),
            CheatCommand("Keep alive", "ServerAliveInterval 60", "Add under a Host entry to stop idle connections from dropping.")
        ))
    )),
    Cheatsheet("tmux", listOf(
        CheatCategory("Sessions", listOf(
            CheatCommand("New session", "tmux new -s <name>", "Start a named session."),
            CheatCommand("List sessions", "tmux ls", "List running sessions."),
            CheatCommand("Attach", "tmux attach -t <name>", "Reattach to a running session."),
            CheatCommand("Detach", "Ctrl+b d", "Leave the session running in the background."),
            CheatCommand("Kill session", "tmux kill-session -t <name>", "End a session and all its windows.")
        )),
        CheatCategory("Windows & panes", listOf(
            CheatCommand("New window", "Ctrl+b c", "Open a new window in the session."),
            CheatCommand("Next / previous window", "Ctrl+b n / Ctrl+b p", "Cycle through the windows."),
            CheatCommand("Rename window", "Ctrl+b ,", "Give the current window a name."),
            CheatCommand("Split left/right", "Ctrl+b %", "Split the pane into two side by side."),
            CheatCommand("Split top/bottom", "Ctrl+b \"", "Split the pane into two stacked panes."),
            CheatCommand("Move between panes", "Ctrl+b arrow", "Focus the pane in the arrow's direction."),
            CheatCommand("Zoom pane", "Ctrl+b z", "Toggle the current pane to full screen."),
            CheatCommand("Scroll mode", "Ctrl+b [", "Scroll back and copy text. Press q to leave.")
        ))
    )),
    Cheatsheet("systemd", listOf(
        CheatCategory("Services", listOf(
            CheatCommand("Status", "systemctl status <unit>", "Show whether a service runs, plus its latest log lines."),
            CheatCommand("Restart", "sudo systemctl restart <unit>", "Stop and start a service. Use start or stop on their own too."),
            CheatCommand("Enable at boot", "sudo systemctl enable --now <unit>", "Start a service now and at every boot."),
            CheatCommand("Disable", "sudo systemctl disable --now <unit>", "Stop a service and keep it from starting at boot."),
            CheatCommand("Running services", "systemctl list-units --type=service --state=running", "List the services that are running."),
            CheatCommand("Failed units", "systemctl --failed", "List units that failed to start."),
            CheatCommand("Reload unit files", "sudo systemctl daemon-reload", "Pick up edited unit files."),
            CheatCommand("Show unit file", "systemctl cat <unit>", "Print the unit file and its drop-ins.")
        )),
        CheatCategory("journalctl", listOf(
            CheatCommand("Follow a service", "journalctl -u <unit> -f", "Stream a service's log as it is written."),
            CheatCommand("Since a time", "journalctl -u <unit> --since \"1 hour ago\"", "Show a service's log for the last hour."),
            CheatCommand("Errors this boot", "journalctl -b -p err", "Show error-level messages since the last boot."),
            CheatCommand("Kernel messages", "journalctl -k", "Show kernel messages, like dmesg."),
            CheatCommand("Disk usage", "journalctl --disk-usage", "Show how much space the journal uses."),
            CheatCommand("Trim the journal", "sudo journalctl --vacuum-time=7d", "Delete journal entries older than 7 days.")
        ))
    )),
    Cheatsheet("curl", listOf(
        CheatCategory("Requests", listOf(
            CheatCommand("Show headers too", "curl -i <url>", "Print the response headers and the body."),
            CheatCommand("Headers only", "curl -I <url>", "Send a HEAD request and print only the headers."),
            CheatCommand("Follow redirects", "curl -L <url>", "Follow 3xx redirects to the final URL."),
            CheatCommand("POST JSON", "curl -X POST -H \"Content-Type: application/json\" -d '{\"key\":\"value\"}' <url>", "Send a JSON body."),
            CheatCommand("Bearer token", "curl -H \"Authorization: Bearer <token>\" <url>", "Call an API with a bearer token."),
            CheatCommand("Basic auth", "curl -u <user>:<password> <url>", "Send HTTP basic authentication."),
            CheatCommand("Upload a file", "curl -F \"file=@<path>\" <url>", "Send a multipart/form-data upload.")
        )),
        CheatCategory("Files & debugging", listOf(
            CheatCommand("Save to file", "curl -o <file> <url>", "Write the body to a named file."),
            CheatCommand("Keep remote name", "curl -O <url>", "Save the file under its name from the URL."),
            CheatCommand("Resume download", "curl -C - -O <url>", "Continue an interrupted download."),
            CheatCommand("Verbose", "curl -v <url>", "Show the TLS handshake, request and response headers."),
            CheatCommand("Skip TLS check", "curl -k <url>", "Accept invalid certificates. For local testing only."),
            CheatCommand("Pin host to IP", "curl --resolve <host>:443:<ip> https://<host>/", "Send the request to a chosen IP without editing DNS."),
            CheatCommand("Status and timing", "curl -s -o /dev/null -w \"%{http_code} %{time_total}s\\n\" <url>", "Print only the status code and total time.")
        ))
    )),
    Cheatsheet("jq", listOf(
        CheatCategory("Reading", listOf(
            CheatCommand("Pretty print", "jq . <file>", "Format JSON with indentation and colors."),
            CheatCommand("Compact", "jq -c . <file>", "Print each JSON value on one line."),
            CheatCommand("Field", "jq '.key' <file>", "Extract one field."),
            CheatCommand("Array item", "jq '.items[0]' <file>", "Get the first element of an array."),
            CheatCommand("Raw string", "jq -r '.name' <file>", "Print strings without quotes."),
            CheatCommand("Keys and length", "jq 'keys, length' <file>", "List an object's keys and count its entries.")
        )),
        CheatCategory("Filtering & shaping", listOf(
            CheatCommand("Filter items", "jq '.[] | select(.age > 30)' <file>", "Keep the array items that match a condition."),
            CheatCommand("Map a field", "jq 'map(.name)' <file>", "Turn an array of objects into an array of one field."),
            CheatCommand("Pick fields", "jq '[.[] | {id, name}]' <file>", "Build smaller objects with only some fields."),
            CheatCommand("Group", "jq 'group_by(.type)' <file>", "Group array items by a field."),
            CheatCommand("Pass a variable", "jq --arg v \"<value>\" '.[] | select(.id == \$v)' <file>", "Use a shell value inside a filter."),
            CheatCommand("Merge files", "jq -s '.' <file1> <file2>", "Read several inputs into one array."),
            CheatCommand("To CSV", "jq -r '.[] | [.id, .name] | @csv' <file>", "Print array items as CSV rows.")
        ))
    )),
    Cheatsheet("OpenSSL", listOf(
        CheatCategory("Certificates", listOf(
            CheatCommand("Inspect certificate", "openssl x509 -in <cert.pem> -noout -text", "Show all fields of a PEM certificate."),
            CheatCommand("Validity dates", "openssl x509 -in <cert.pem> -noout -dates", "Show when a certificate starts and expires."),
            CheatCommand("Fingerprint", "openssl x509 -in <cert.pem> -noout -fingerprint -sha256", "Show the SHA-256 fingerprint."),
            CheatCommand("Server certificate", "openssl s_client -connect <host>:443 -servername <host>", "Open a TLS connection and print the server's chain."),
            CheatCommand("DER to PEM", "openssl x509 -inform der -in <cert.der> -out <cert.pem>", "Convert a binary certificate to PEM.")
        )),
        CheatCategory("Keys & requests", listOf(
            CheatCommand("RSA key", "openssl genrsa -out <key.pem> 4096", "Generate a 4096-bit RSA private key."),
            CheatCommand("Ed25519 key", "openssl genpkey -algorithm ed25519 -out <key.pem>", "Generate an Ed25519 private key."),
            CheatCommand("Public key", "openssl pkey -in <key.pem> -pubout", "Print the public key of a private key."),
            CheatCommand("Signing request", "openssl req -new -key <key.pem> -out <csr.pem>", "Create a CSR to send to a certificate authority."),
            CheatCommand("Inspect request", "openssl req -in <csr.pem> -noout -text", "Show the fields of a CSR."),
            CheatCommand("Self-signed cert", "openssl req -x509 -newkey rsa:4096 -nodes -keyout key.pem -out cert.pem -days 365 -subj \"/CN=localhost\"", "Create a key and a self-signed certificate in one step."),
            CheatCommand("PKCS#12 bundle", "openssl pkcs12 -export -inkey <key.pem> -in <cert.pem> -out <bundle.p12>", "Pack a key and certificate into one .p12 file.")
        )),
        CheatCategory("Utilities", listOf(
            CheatCommand("Random bytes", "openssl rand -hex 32", "Print 32 random bytes as hex."),
            CheatCommand("File hash", "openssl dgst -sha256 <file>", "Compute a file's SHA-256 digest."),
            CheatCommand("Base64 encode", "openssl base64 -in <file>", "Encode a file as Base64.")
        ))
    )),
    Cheatsheet("Docker", listOf(
        CheatCategory("Containers", listOf(
            CheatCommand("Run detached", "docker run -d -p 80:80 <image>", "Run a container in the background and map port 80."),
            CheatCommand("List running", "docker ps", "List all currently running containers."),
            CheatCommand("List all", "docker ps -a", "List all containers, including stopped ones."),
            CheatCommand("Stop container", "docker stop <id>", "Gracefully stop a running container."),
            CheatCommand("Remove container", "docker rm -f <id>", "Forcefully remove a running or stopped container."),
            CheatCommand("View logs", "docker logs -f <id>", "Fetch and follow the logs of a container."),
            CheatCommand("Execute shell", "docker exec -it <id> /bin/sh", "Run an interactive shell session inside a running container.")
        )),
        CheatCategory("Images", listOf(
            CheatCommand("List images", "docker images", "List all locally available Docker images."),
            CheatCommand("Remove image", "docker rmi <id>", "Remove a specific image from the local storage."),
            CheatCommand("Build image", "docker build -t <name>:<tag> .", "Build an image from a Dockerfile in the current directory."),
            CheatCommand("Pull image", "docker pull <image>", "Download an image from a container registry."),
            CheatCommand("Prune system", "docker system prune -a", "Remove all unused containers, networks, images, and optionally, volumes.")
        )),
        CheatCategory("Docker Compose", listOf(
            CheatCommand("Start services", "docker-compose up -d", "Create and start containers defined in docker-compose.yml in the background."),
            CheatCommand("Stop services", "docker-compose down", "Stop and remove containers and networks. Add -v to also remove volumes, --rmi all to remove images."),
            CheatCommand("View Compose logs", "docker-compose logs -f", "Follow log output from services defined in the compose file.")
        ))
    )),
    Cheatsheet("Kubernetes", listOf(
        CheatCategory("Pods", listOf(
            CheatCommand("List pods", "kubectl get pods -A", "List all pods in all namespaces."),
            CheatCommand("Describe pod", "kubectl describe pod <name>", "Show detailed information about a specific pod."),
            CheatCommand("Delete pod", "kubectl delete pod <name>", "Delete a pod by name."),
            CheatCommand("Pod logs", "kubectl logs <name> -f", "Follow the logs of a container in a pod."),
            CheatCommand("Execute in pod", "kubectl exec -it <name> -- /bin/sh", "Execute an interactive shell inside a running pod.")
        )),
        CheatCategory("Deployments & services", listOf(
            CheatCommand("List deployments", "kubectl get deployments", "List all deployments in the current namespace."),
            CheatCommand("Scale deployment", "kubectl scale deployment <name> --replicas=3", "Adjust the number of replicas running for a deployment."),
            CheatCommand("Rollout status", "kubectl rollout status deployment/<name>", "Check the progress of a deployment rollout."),
            CheatCommand("List services", "kubectl get svc", "List all services in the current namespace."),
            CheatCommand("Port forward", "kubectl port-forward svc/<name> 8080:80", "Forward a local port to a port on a Kubernetes service.")
        )),
        CheatCategory("Cluster", listOf(
            CheatCommand("Cluster info", "kubectl cluster-info", "Display cluster endpoint and configuration information."),
            CheatCommand("List nodes", "kubectl get nodes", "List all nodes participating in the cluster."),
            CheatCommand("Change context", "kubectl config use-context <name>", "Switch between different cluster contexts.")
        ))
    )),
    Cheatsheet("Helm", listOf(
        CheatCategory("Repositories", listOf(
            CheatCommand("Add repo", "helm repo add <name> <url>", "Register a chart repository."),
            CheatCommand("Update repos", "helm repo update", "Fetch the latest chart lists."),
            CheatCommand("Search charts", "helm search repo <keyword>", "Find charts in the added repositories."),
            CheatCommand("Default values", "helm show values <chart>", "Print a chart's configurable values.")
        )),
        CheatCategory("Releases", listOf(
            CheatCommand("Install", "helm install <release> <chart> -n <namespace> --create-namespace", "Install a chart as a named release."),
            CheatCommand("Install or upgrade", "helm upgrade --install <release> <chart> -f values.yaml", "Install, or upgrade if the release exists."),
            CheatCommand("List releases", "helm list -A", "List releases in all namespaces."),
            CheatCommand("Release history", "helm history <release>", "Show the revisions of a release."),
            CheatCommand("Roll back", "helm rollback <release> <revision>", "Return a release to an earlier revision."),
            CheatCommand("Uninstall", "helm uninstall <release>", "Remove a release and its resources."),
            CheatCommand("Render locally", "helm template <release> <chart>", "Print the manifests without installing them.")
        ))
    )),
    Cheatsheet("Gradle / Android", listOf(
        CheatCategory("Builds", listOf(
            CheatCommand("List tasks", "./gradlew tasks", "Show the tasks you can run."),
            CheatCommand("Debug APK", "./gradlew assembleDebug", "Build a debug APK."),
            CheatCommand("Release APK", "./gradlew assembleRelease", "Build a release APK."),
            CheatCommand("Release bundle", "./gradlew bundleRelease", "Build an Android App Bundle (.aab)."),
            CheatCommand("Install on device", "./gradlew installDebug", "Build and install the debug APK over ADB."),
            CheatCommand("Unit tests", "./gradlew testDebugUnitTest", "Run the JVM unit tests."),
            CheatCommand("Device tests", "./gradlew connectedAndroidTest", "Run instrumented tests on a connected device."),
            CheatCommand("Lint", "./gradlew lintDebug", "Run Android lint and write an HTML report."),
            CheatCommand("Clean", "./gradlew clean", "Delete the build directories.")
        )),
        CheatCategory("Diagnostics", listOf(
            CheatCommand("Signing report", "./gradlew signingReport", "Show the SHA-1 and SHA-256 of each signing config."),
            CheatCommand("Dependency tree", "./gradlew :app:dependencies --configuration releaseRuntimeClasspath", "Print the resolved dependency tree."),
            CheatCommand("Why this version", "./gradlew :app:dependencyInsight --dependency <name> --configuration releaseRuntimeClasspath", "Explain why a dependency resolved to its version."),
            CheatCommand("Stack trace", "./gradlew <task> --stacktrace", "Show the full stack trace of a build failure."),
            CheatCommand("Stop daemons", "./gradlew --stop", "Stop all Gradle daemons."),
            CheatCommand("Update wrapper", "./gradlew wrapper --gradle-version <version>", "Change the project's Gradle version.")
        )),
        CheatCategory("Signing", listOf(
            CheatCommand("New keystore", "keytool -genkeypair -v -keystore <file>.jks -alias <alias> -keyalg RSA -keysize 4096 -validity 10000", "Create an upload or release keystore."),
            CheatCommand("Inspect keystore", "keytool -list -v -keystore <file>.jks", "Show the keys and certificate fingerprints."),
            CheatCommand("Verify APK", "apksigner verify --print-certs <app.apk>", "Check an APK's signature and print its certificates.")
        ))
    )),
    Cheatsheet("Regex", listOf(
        CheatCategory("Character classes", listOf(
            CheatCommand("Any character", ".", "Matches any single character except newline."),
            CheatCommand("Digit", "\\d", "Matches any digit (equivalent to [0-9])."),
            CheatCommand("Word character", "\\w", "Matches any alphanumeric character or underscore (equivalent to [a-zA-Z0-9_])."),
            CheatCommand("Whitespace", "\\s", "Matches any whitespace character (space, tab, newline)."),
            CheatCommand("Negated classes", "\\D, \\W, \\S", "Matches non-digit, non-word, or non-whitespace characters respectively.")
        )),
        CheatCategory("Quantifiers", listOf(
            CheatCommand("Zero or more", "*", "Matches the preceding element zero or more times."),
            CheatCommand("One or more", "+", "Matches the preceding element one or more times."),
            CheatCommand("Optional", "?", "Matches the preceding element zero or one time."),
            CheatCommand("Exact count", "{N}", "Matches exactly N occurrences of the preceding element."),
            CheatCommand("Range count", "{N,M}", "Matches between N and M occurrences of the preceding element.")
        )),
        CheatCategory("Anchors & boundaries", listOf(
            CheatCommand("Start of string", "^", "Matches the beginning of the string or line."),
            CheatCommand("End of string", "$", "Matches the end of the string or line."),
            CheatCommand("Word boundary", "\\b", "Matches a position between a word character and a non-word character.")
        )),
        CheatCategory("Groups", listOf(
            CheatCommand("Capture group", "(abc)", "Groups multiple tokens together and creates a capture group for extracting text."),
            CheatCommand("Non-capturing group", "(?:abc)", "Groups tokens together without creating a capture group."),
            CheatCommand("Alternation", "a|b", "Matches either the pattern 'a' or the pattern 'b' (Boolean OR).")
        ))
    )),
    Cheatsheet("Vim", listOf(
        CheatCategory("Modes & exiting", listOf(
            CheatCommand("Insert mode", "i", "Enter Insert mode before the cursor."),
            CheatCommand("Normal mode", "Esc", "Exit Insert or Visual mode and return to Normal mode."),
            CheatCommand("Save (write)", ":w", "Save the current file."),
            CheatCommand("Quit", ":q", "Quit Vim (fails if there are unsaved changes)."),
            CheatCommand("Save & quit", ":wq", "Save the current file and quit Vim."),
            CheatCommand("Force quit", ":q!", "Quit Vim immediately and discard any unsaved changes.")
        )),
        CheatCategory("Navigation", listOf(
            CheatCommand("Basic movement", "h j k l", "Move cursor left, down, up, and right respectively."),
            CheatCommand("Word forward", "w", "Move forward to the beginning of the next word."),
            CheatCommand("Word backward", "b", "Move backward to the beginning of the previous word."),
            CheatCommand("Start/end of line", "0 / $", "Move to the very beginning (0) or end ($) of the current line."),
            CheatCommand("File top/bottom", "gg / G", "Move to the first line (gg) or the last line (G) of the document.")
        )),
        CheatCategory("Editing", listOf(
            CheatCommand("Delete line", "dd", "Delete the current line and copy it to the clipboard (register)."),
            CheatCommand("Delete word", "dw", "Delete from the cursor to the end of the current word."),
            CheatCommand("Undo / redo", "u / Ctrl+r", "Undo the last action (u) or redo the last undone action (Ctrl+r)."),
            CheatCommand("Copy line", "yy", "Yank (copy) the current line."),
            CheatCommand("Paste", "p", "Paste the contents of the clipboard after the cursor.")
        )),
        CheatCategory("Search & replace", listOf(
            CheatCommand("Search forward", "/pattern", "Search forward in the document for the specified pattern."),
            CheatCommand("Search backward", "?pattern", "Search backward in the document for the specified pattern."),
            CheatCommand("Next/prev match", "n / N", "Jump to the next (n) or previous (N) search match."),
            CheatCommand("Global replace", ":%s/old/new/g", "Replace all occurrences of 'old' with 'new' throughout the entire file.")
        ))
    )),
    Cheatsheet("SQL", listOf(
        CheatCategory("Querying data", listOf(
            CheatCommand("Select all", "SELECT * FROM table;", "Retrieve all columns and rows from a table."),
            CheatCommand("Select distinct", "SELECT DISTINCT col FROM table;", "Retrieve unique values from a specific column."),
            CheatCommand("Filter results", "SELECT * FROM table WHERE col = 'val';", "Filter rows based on a specific condition."),
            CheatCommand("Sort results", "SELECT * FROM table ORDER BY col DESC;", "Sort the result set in descending order based on a column.")
        )),
        CheatCategory("Data modification", listOf(
            CheatCommand("Insert row", "INSERT INTO table (col1, col2) VALUES (v1, v2);", "Add a new record into a table."),
            CheatCommand("Update row", "UPDATE table SET col = new_val WHERE condition;", "Modify existing records in a table matching a condition."),
            CheatCommand("Delete row", "DELETE FROM table WHERE condition;", "Remove records from a table matching a condition.")
        )),
        CheatCategory("Joins & aggregation", listOf(
            CheatCommand("Inner join", "SELECT * FROM t1 INNER JOIN t2 ON t1.id = t2.id;", "Return rows that have matching values in both tables."),
            CheatCommand("Left join", "SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.id;", "Return all rows from the left table, and matched rows from the right."),
            CheatCommand("Count rows", "SELECT COUNT(*) FROM table;", "Count the total number of rows in a table."),
            CheatCommand("Group by", "SELECT col, COUNT(*) FROM table GROUP BY col;", "Group rows that have the same values into summary rows.")
        ))
    )),
    Cheatsheet("ADB", listOf(
        CheatCategory("Devices & connection", listOf(
            CheatCommand("List devices", "adb devices", "List all connected physical devices and emulators."),
            CheatCommand("Connect Wi-Fi", "adb connect <ip>:<port>", "Connect to an Android device over Wi-Fi TCP/IP."),
            CheatCommand("Restart server", "adb kill-server && adb start-server", "Restart the ADB background daemon.")
        )),
        CheatCategory("App management", listOf(
            CheatCommand("Install APK", "adb install app.apk", "Install an Android application package to the device."),
            CheatCommand("Uninstall app", "adb uninstall com.package.name", "Uninstall an application using its package name."),
            CheatCommand("Clear app data", "adb shell pm clear com.package.name", "Clear all data and cache for a specific application.")
        )),
        CheatCategory("Files & shell", listOf(
            CheatCommand("Push file", "adb push local.txt /sdcard/", "Copy a file from your computer to the Android device."),
            CheatCommand("Pull file", "adb pull /sdcard/file.txt .", "Copy a file from the Android device to your computer."),
            CheatCommand("Open shell", "adb shell", "Open an interactive Linux command-line shell on the device."),
            CheatCommand("View logs", "adb logcat", "View the system and application log output in real-time.")
        ))
    )),
    Cheatsheet("npm / pip", listOf(
        CheatCategory("npm", listOf(
            CheatCommand("Install", "npm install", "Install the dependencies in package.json."),
            CheatCommand("Clean install", "npm ci", "Install exactly what package-lock.json lists. Use it in CI."),
            CheatCommand("Add package", "npm install <pkg>", "Add a runtime dependency."),
            CheatCommand("Add dev package", "npm install -D <pkg>", "Add a development dependency."),
            CheatCommand("Run script", "npm run <script>", "Run a script from package.json."),
            CheatCommand("Outdated", "npm outdated", "List packages with newer versions."),
            CheatCommand("Why installed", "npm ls <pkg>", "Show which packages depend on a package."),
            CheatCommand("Run without install", "npx <command>", "Run a package's binary without installing it globally."),
            CheatCommand("Fix advisories", "npm audit fix", "Upgrade packages with known vulnerabilities.")
        )),
        CheatCategory("pnpm", listOf(
            CheatCommand("Install", "pnpm install", "Install the dependencies."),
            CheatCommand("Add package", "pnpm add <pkg>", "Add a dependency. Use -D for a dev dependency."),
            CheatCommand("Run script", "pnpm <script>", "Run a script from package.json."),
            CheatCommand("Run without install", "pnpm dlx <command>", "Like npx: run a package once.")
        )),
        CheatCategory("pip", listOf(
            CheatCommand("Create venv", "python -m venv .venv", "Create a virtual environment in .venv."),
            CheatCommand("Activate venv", "source .venv/bin/activate", "Use the venv in this shell. On Windows: .venv\\Scripts\\activate"),
            CheatCommand("Install package", "pip install <pkg>", "Install a package into the active environment."),
            CheatCommand("Install from file", "pip install -r requirements.txt", "Install every package listed in a requirements file."),
            CheatCommand("Freeze", "pip freeze > requirements.txt", "Write the installed versions to a requirements file."),
            CheatCommand("Outdated", "pip list --outdated", "List packages with newer versions."),
            CheatCommand("Package info", "pip show <pkg>", "Show version, location and dependencies of a package.")
        ))
    )),
    Cheatsheet("PowerShell", listOf(
        CheatCategory("Files", listOf(
            CheatCommand("List files", "Get-ChildItem -Force", "List files, including hidden ones (alias ls, dir)."),
            CheatCommand("Find files", "Get-ChildItem -Recurse -Filter *.log", "Search subfolders for matching file names."),
            CheatCommand("Tail a file", "Get-Content <file> -Tail 50 -Wait", "Show the last 50 lines and follow new ones."),
            CheatCommand("Search text", "Select-String -Path *.txt -Pattern \"<text>\"", "Find lines that match a pattern, like grep."),
            CheatCommand("Copy folder", "Copy-Item <src> <dest> -Recurse", "Copy a folder and its contents."),
            CheatCommand("Delete folder", "Remove-Item <path> -Recurse -Force", "Delete a folder and its contents. Use with caution."),
            CheatCommand("New folder", "New-Item -ItemType Directory <dir>", "Create a folder.")
        )),
        CheatCategory("System", listOf(
            CheatCommand("Top processes", "Get-Process | Sort-Object CPU -Descending | Select-Object -First 10", "Show the ten processes using the most CPU."),
            CheatCommand("Stop process", "Stop-Process -Name <name>", "End processes by name."),
            CheatCommand("Running services", "Get-Service | Where-Object Status -eq Running", "List running services."),
            CheatCommand("Listening ports", "Get-NetTCPConnection -State Listen", "List TCP ports that accept connections."),
            CheatCommand("Test a port", "Test-NetConnection <host> -Port <port>", "Check whether a host accepts connections on a port."),
            CheatCommand("Show PATH", "\$env:PATH -split ';'", "Print each PATH entry on its own line."),
            CheatCommand("Where is a command", "Get-Command <name>", "Show where a command comes from, like which."),
            CheatCommand("Examples", "Get-Help <cmdlet> -Examples", "Show usage examples for a cmdlet.")
        ))
    )),
)
