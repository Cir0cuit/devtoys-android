package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor

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
        ))
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
    ))
)

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor(
        "cheatsheet",
        "Cheatsheets",
        "Look up common Git, shell and tool commands",
        Icons.AutoMirrored.Filled.MenuBook,
        ToolCategory.NETWORK_SYSTEM,
        listOf("cheat", "sheet", "commands", "reference")
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        var selected by rememberSaveable { mutableStateOf(CHEATSHEETS.first().title) }
        var query by rememberSaveable { mutableStateOf("") }
        var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current

        val activeSheet = CHEATSHEETS.find { it.title == selected } ?: CHEATSHEETS.first()
        val q = query.trim()
        val categories = if (q.isEmpty()) activeSheet.categories else activeSheet.categories.mapNotNull { cat ->
            cat.commands.filter { cmd ->
                listOf(cmd.title, cmd.command, cmd.description).any { it.contains(q, ignoreCase = true) }
            }.takeIf { it.isNotEmpty() }?.let { cat.copy(commands = it) }
        }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CHEATSHEETS.forEach { sheet ->
                    FilterChip(
                        selected = selected == sheet.title,
                        onClick = { selected = sheet.title },
                        label = { Text(sheet.title) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SearchField(query, { query = it }, "Search ${activeSheet.title} commands")

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (categories.isEmpty()) {
                    item {
                        Text(
                            "No ${activeSheet.title} commands match \"$q\". Try another word or pick a different sheet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                categories.forEach { category ->
                    val key = "${activeSheet.title}/${category.name}"
                    // While searching every match is shown, so collapsing is disabled.
                    val isExpanded = q.isNotEmpty() || key !in collapsed
                    item(key = key) {
                        SectionHeader(
                            category.name,
                            isExpanded,
                            onToggle = if (q.isNotEmpty()) null else {
                                { collapsed = if (isExpanded) collapsed + key else collapsed - key }
                            }
                        )
                    }
                    if (isExpanded) {
                        items(category.commands, key = { "$key/${it.title}" }) { cmd ->
                            CommandRow(cmd) {
                                clipboardManager.setText(AnnotatedString(cmd.command))
                                confirmCopied(context, "Command")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CommandRow(cmd: CheatCommand, onCopy: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(cmd.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            cmd.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        Surface(
            color = LocalSlabColor.current,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Copy command", onClick = onCopy)
        ) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(cmd.command, style = CodeTextStyle, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(18.dp)
                )
            }
        }
    }
}
