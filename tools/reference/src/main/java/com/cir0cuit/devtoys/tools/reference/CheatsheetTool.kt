package com.cir0cuit.devtoys.tools.reference

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        CheatCategory("Branching & Merging", listOf(
            CheatCommand("Create Branch", "git checkout -b <name>", "Create a new branch and immediately switch to it."),
            CheatCommand("Switch Branch", "git checkout <name>", "Switch the working tree to the specified branch."),
            CheatCommand("List Branches", "git branch -a", "List all local and remote-tracking branches."),
            CheatCommand("Merge", "git merge <branch>", "Merge the specified branch's history into the current branch."),
            CheatCommand("Delete Branch", "git branch -d <name>", "Delete the specified local branch safely (prevents deleting unmerged changes).")
        )),
        CheatCategory("Remote", listOf(
            CheatCommand("Add Remote", "git remote add origin <url>", "Add a new remote repository to track."),
            CheatCommand("Fetch", "git fetch", "Download objects and refs from another repository without merging them."),
            CheatCommand("Pull", "git pull origin <branch>", "Fetch from and integrate with another repository or a local branch."),
            CheatCommand("Push", "git push origin <branch>", "Update remote refs along with associated objects.")
        )),
        CheatCategory("Undoing", listOf(
            CheatCommand("Soft Reset", "git reset --soft HEAD~1", "Undo the last commit but keep the changes staged in your working directory."),
            CheatCommand("Hard Reset", "git reset --hard HEAD~1", "Completely discard the last commit and all uncommitted changes."),
            CheatCommand("Revert", "git revert <commit>", "Create a new commit that undoes the changes from a previous commit safely."),
            CheatCommand("Discard File", "git restore <file>", "Discard local modifications to a specific file in the working directory.")
        )),
        CheatCategory("Stashing", listOf(
            CheatCommand("Stash Changes", "git stash", "Temporarily save uncommitted changes and revert the working directory to match the HEAD commit."),
            CheatCommand("Apply Stash", "git stash pop", "Apply the most recently stashed changes and remove them from the stash list."),
            CheatCommand("List Stashes", "git stash list", "Show all currently stashed changes.")
        ))
    )),
    Cheatsheet("Linux / Bash", listOf(
        CheatCategory("File System", listOf(
            CheatCommand("List Files", "ls -lah", "List files and directories in long format, including hidden files and human-readable sizes."),
            CheatCommand("Change Directory", "cd /path/to/dir", "Change the current working directory."),
            CheatCommand("Print Working Directory", "pwd", "Print the absolute path of the current working directory."),
            CheatCommand("Make Directory", "mkdir -p dir/sub", "Create directories and any necessary parent directories."),
            CheatCommand("Remove Directory", "rm -rf <dir>", "Forcefully remove a directory and its contents recursively. Use with caution."),
            CheatCommand("Copy", "cp -r <src> <dest>", "Copy files and directories recursively."),
            CheatCommand("Move / Rename", "mv <src> <dest>", "Move or rename files and directories."),
            CheatCommand("Find File", "find . -name \"*.txt\"", "Search for files by name within the current directory hierarchy.")
        )),
        CheatCategory("Process Management", listOf(
            CheatCommand("List Processes", "ps aux", "Display a snapshot of all currently running processes."),
            CheatCommand("Interactive Monitor", "top", "Display real-time system summary and running processes (use htop if available)."),
            CheatCommand("Kill by PID", "kill -9 <pid>", "Forcefully terminate a process using its Process ID."),
            CheatCommand("Kill by Name", "killall <name>", "Kill all processes matching a specific name."),
            CheatCommand("Background Job", "<command> &", "Run a command in the background, freeing up the terminal.")
        )),
        CheatCategory("Networking", listOf(
            CheatCommand("Ping", "ping <host>", "Send ICMP ECHO_REQUEST to network hosts to check connectivity."),
            CheatCommand("Open Ports", "netstat -tuln", "Show all listening TCP and UDP ports and their corresponding services."),
            CheatCommand("Download File", "wget <url>", "Download a file from the internet via HTTP, HTTPS, or FTP."),
            CheatCommand("HTTP Request", "curl -X GET <url>", "Transfer data from or to a server using HTTP.")
        )),
        CheatCategory("System & Archives", listOf(
            CheatCommand("Disk Space", "df -h", "Show file system disk space usage in human-readable format."),
            CheatCommand("Memory Usage", "free -m", "Display amount of free and used memory in the system in megabytes."),
            CheatCommand("Create Tarball", "tar -czvf archive.tar.gz /dir", "Compress a directory into a gzip-compressed tar archive."),
            CheatCommand("Extract Tarball", "tar -xzvf archive.tar.gz", "Extract a gzip-compressed tar archive into the current directory.")
        ))
    )),
    Cheatsheet("Docker", listOf(
        CheatCategory("Containers", listOf(
            CheatCommand("Run Detached", "docker run -d -p 80:80 <image>", "Run a container in the background and map port 80."),
            CheatCommand("List Running", "docker ps", "List all currently running containers."),
            CheatCommand("List All", "docker ps -a", "List all containers, including stopped ones."),
            CheatCommand("Stop Container", "docker stop <id>", "Gracefully stop a running container."),
            CheatCommand("Remove Container", "docker rm -f <id>", "Forcefully remove a running or stopped container."),
            CheatCommand("View Logs", "docker logs -f <id>", "Fetch and follow the logs of a container."),
            CheatCommand("Execute Shell", "docker exec -it <id> /bin/sh", "Run an interactive shell session inside a running container.")
        )),
        CheatCategory("Images", listOf(
            CheatCommand("List Images", "docker images", "List all locally available Docker images."),
            CheatCommand("Remove Image", "docker rmi <id>", "Remove a specific image from the local storage."),
            CheatCommand("Build Image", "docker build -t <name>:<tag> .", "Build an image from a Dockerfile in the current directory."),
            CheatCommand("Pull Image", "docker pull <image>", "Download an image from a container registry."),
            CheatCommand("Prune System", "docker system prune -a", "Remove all unused containers, networks, images, and optionally, volumes.")
        )),
        CheatCategory("Docker Compose", listOf(
            CheatCommand("Start Services", "docker-compose up -d", "Create and start containers defined in docker-compose.yml in the background."),
            CheatCommand("Stop Services", "docker-compose down", "Stop and remove containers and networks. Add -v to also remove volumes, --rmi all to remove images."),
            CheatCommand("View Compose Logs", "docker-compose logs -f", "Follow log output from services defined in the compose file.")
        ))
    )),
    Cheatsheet("Kubernetes", listOf(
        CheatCategory("Pods", listOf(
            CheatCommand("List Pods", "kubectl get pods -A", "List all pods in all namespaces."),
            CheatCommand("Describe Pod", "kubectl describe pod <name>", "Show detailed information about a specific pod."),
            CheatCommand("Delete Pod", "kubectl delete pod <name>", "Delete a pod by name."),
            CheatCommand("Pod Logs", "kubectl logs <name> -f", "Follow the logs of a container in a pod."),
            CheatCommand("Execute in Pod", "kubectl exec -it <name> -- /bin/sh", "Execute an interactive shell inside a running pod.")
        )),
        CheatCategory("Deployments & Services", listOf(
            CheatCommand("List Deployments", "kubectl get deployments", "List all deployments in the current namespace."),
            CheatCommand("Scale Deployment", "kubectl scale deployment <name> --replicas=3", "Adjust the number of replicas running for a deployment."),
            CheatCommand("Rollout Status", "kubectl rollout status deployment/<name>", "Check the progress of a deployment rollout."),
            CheatCommand("List Services", "kubectl get svc", "List all services in the current namespace."),
            CheatCommand("Port Forward", "kubectl port-forward svc/<name> 8080:80", "Forward a local port to a port on a Kubernetes service.")
        )),
        CheatCategory("Cluster", listOf(
            CheatCommand("Cluster Info", "kubectl cluster-info", "Display cluster endpoint and configuration information."),
            CheatCommand("List Nodes", "kubectl get nodes", "List all nodes participating in the cluster."),
            CheatCommand("Change Context", "kubectl config use-context <name>", "Switch between different cluster contexts.")
        ))
    )),
    Cheatsheet("Regex", listOf(
        CheatCategory("Character Classes", listOf(
            CheatCommand("Any Character", ".", "Matches any single character except newline."),
            CheatCommand("Digit", "\\d", "Matches any digit (equivalent to [0-9])."),
            CheatCommand("Word Character", "\\w", "Matches any alphanumeric character or underscore (equivalent to [a-zA-Z0-9_])."),
            CheatCommand("Whitespace", "\\s", "Matches any whitespace character (space, tab, newline)."),
            CheatCommand("Negated Classes", "\\D, \\W, \\S", "Matches non-digit, non-word, or non-whitespace characters respectively.")
        )),
        CheatCategory("Quantifiers", listOf(
            CheatCommand("Zero or More", "*", "Matches the preceding element zero or more times."),
            CheatCommand("One or More", "+", "Matches the preceding element one or more times."),
            CheatCommand("Optional", "?", "Matches the preceding element zero or one time."),
            CheatCommand("Exact Count", "{N}", "Matches exactly N occurrences of the preceding element."),
            CheatCommand("Range Count", "{N,M}", "Matches between N and M occurrences of the preceding element.")
        )),
        CheatCategory("Anchors & Boundaries", listOf(
            CheatCommand("Start of String", "^", "Matches the beginning of the string or line."),
            CheatCommand("End of String", "$", "Matches the end of the string or line."),
            CheatCommand("Word Boundary", "\\b", "Matches a position between a word character and a non-word character.")
        )),
        CheatCategory("Groups", listOf(
            CheatCommand("Capture Group", "(abc)", "Groups multiple tokens together and creates a capture group for extracting text."),
            CheatCommand("Non-capturing Group", "(?:abc)", "Groups tokens together without creating a capture group."),
            CheatCommand("Alternation", "a|b", "Matches either the pattern 'a' or the pattern 'b' (Boolean OR).")
        ))
    )),
    Cheatsheet("Vim", listOf(
        CheatCategory("Modes & Exiting", listOf(
            CheatCommand("Insert Mode", "i", "Enter Insert mode before the cursor."),
            CheatCommand("Normal Mode", "Esc", "Exit Insert or Visual mode and return to Normal mode."),
            CheatCommand("Save (Write)", ":w", "Save the current file."),
            CheatCommand("Quit", ":q", "Quit Vim (fails if there are unsaved changes)."),
            CheatCommand("Save & Quit", ":wq", "Save the current file and quit Vim."),
            CheatCommand("Force Quit", ":q!", "Quit Vim immediately and discard any unsaved changes.")
        )),
        CheatCategory("Navigation", listOf(
            CheatCommand("Basic Movement", "h j k l", "Move cursor Left, Down, Up, and Right respectively."),
            CheatCommand("Word Forward", "w", "Move forward to the beginning of the next word."),
            CheatCommand("Word Backward", "b", "Move backward to the beginning of the previous word."),
            CheatCommand("Start/End of Line", "0 / $", "Move to the very beginning (0) or end ($) of the current line."),
            CheatCommand("File Top/Bottom", "gg / G", "Move to the first line (gg) or the last line (G) of the document.")
        )),
        CheatCategory("Editing", listOf(
            CheatCommand("Delete Line", "dd", "Delete the current line and copy it to the clipboard (register)."),
            CheatCommand("Delete Word", "dw", "Delete from the cursor to the end of the current word."),
            CheatCommand("Undo / Redo", "u / Ctrl+r", "Undo the last action (u) or redo the last undone action (Ctrl+r)."),
            CheatCommand("Copy Line", "yy", "Yank (copy) the current line."),
            CheatCommand("Paste", "p", "Paste the contents of the clipboard after the cursor.")
        )),
        CheatCategory("Search & Replace", listOf(
            CheatCommand("Search Forward", "/pattern", "Search forward in the document for the specified pattern."),
            CheatCommand("Search Backward", "?pattern", "Search backward in the document for the specified pattern."),
            CheatCommand("Next/Prev Match", "n / N", "Jump to the next (n) or previous (N) search match."),
            CheatCommand("Global Replace", ":%s/old/new/g", "Replace all occurrences of 'old' with 'new' throughout the entire file.")
        ))
    )),
    Cheatsheet("SQL", listOf(
        CheatCategory("Querying Data", listOf(
            CheatCommand("Select All", "SELECT * FROM table;", "Retrieve all columns and rows from a table."),
            CheatCommand("Select Distinct", "SELECT DISTINCT col FROM table;", "Retrieve unique values from a specific column."),
            CheatCommand("Filter Results", "SELECT * FROM table WHERE col = 'val';", "Filter rows based on a specific condition."),
            CheatCommand("Sort Results", "SELECT * FROM table ORDER BY col DESC;", "Sort the result set in descending order based on a column.")
        )),
        CheatCategory("Data Modification", listOf(
            CheatCommand("Insert Row", "INSERT INTO table (col1, col2) VALUES (v1, v2);", "Add a new record into a table."),
            CheatCommand("Update Row", "UPDATE table SET col = new_val WHERE condition;", "Modify existing records in a table matching a condition."),
            CheatCommand("Delete Row", "DELETE FROM table WHERE condition;", "Remove records from a table matching a condition.")
        )),
        CheatCategory("Joins & Aggregation", listOf(
            CheatCommand("Inner Join", "SELECT * FROM t1 INNER JOIN t2 ON t1.id = t2.id;", "Return rows that have matching values in both tables."),
            CheatCommand("Left Join", "SELECT * FROM t1 LEFT JOIN t2 ON t1.id = t2.id;", "Return all rows from the left table, and matched rows from the right."),
            CheatCommand("Count Rows", "SELECT COUNT(*) FROM table;", "Count the total number of rows in a table."),
            CheatCommand("Group By", "SELECT col, COUNT(*) FROM table GROUP BY col;", "Group rows that have the same values into summary rows.")
        ))
    )),
    Cheatsheet("ADB", listOf(
        CheatCategory("Devices & Connection", listOf(
            CheatCommand("List Devices", "adb devices", "List all connected physical devices and emulators."),
            CheatCommand("Connect Wi-Fi", "adb connect <ip>:<port>", "Connect to an Android device over Wi-Fi TCP/IP."),
            CheatCommand("Restart Server", "adb kill-server && adb start-server", "Restart the ADB background daemon.")
        )),
        CheatCategory("App Management", listOf(
            CheatCommand("Install APK", "adb install app.apk", "Install an Android application package to the device."),
            CheatCommand("Uninstall App", "adb uninstall com.package.name", "Uninstall an application using its package name."),
            CheatCommand("Clear App Data", "adb shell pm clear com.package.name", "Clear all data and cache for a specific application.")
        )),
        CheatCategory("Files & Shell", listOf(
            CheatCommand("Push File", "adb push local.txt /sdcard/", "Copy a file from your computer to the Android device."),
            CheatCommand("Pull File", "adb pull /sdcard/file.txt .", "Copy a file from the Android device to your computer."),
            CheatCommand("Open Shell", "adb shell", "Open an interactive Linux command-line shell on the device."),
            CheatCommand("View Logs", "adb logcat", "View the system and application log output in real-time.")
        ))
    ))
)

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor(
        "cheatsheet",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.MenuBook,
        ToolCategory.NETWORK_SYSTEM,
        listOf("cheat", "sheet", "commands", "reference")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var selected by rememberSaveable { mutableStateOf(CHEATSHEETS.first().title) }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        
        var collapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
        
        ToolScaffold("Cheatsheets", modifier = modifier, scrollable = false) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(CHEATSHEETS) { sheet ->
                    FilterChip(
                        selected = selected == sheet.title,
                        onClick = { selected = sheet.title },
                        label = { Text(sheet.title) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val activeSheet = CHEATSHEETS.find { it.title == selected }
            
            if (activeSheet != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    activeSheet.categories.forEach { category ->
                        val isExpanded = category.name !in collapsed
                        item(key = category.name) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { collapsed = if (isExpanded) collapsed + category.name else collapsed - category.name },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }
                        }
                        
                        if (isExpanded) {
                            items(category.commands, key = { it.title }) { cmd ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(cmd.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(cmd.command))
                                                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Command", modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = cmd.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = cmd.command,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}