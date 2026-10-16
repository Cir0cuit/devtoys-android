package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlin.random.Random

internal class Port(val number: Int, val protocol: String, val service: String, val note: String)

private const val TCP = "TCP"
private const val UDP = "UDP"
private const val BOTH = "TCP/UDP"

internal val PORTS = listOf(
    Port(20, TCP, "ftp-data", "FTP data transfer"),
    Port(21, TCP, "ftp", "FTP control"),
    Port(22, TCP, "ssh", "SSH, SFTP and scp"),
    Port(23, TCP, "telnet", "Telnet, unencrypted"),
    Port(25, TCP, "smtp", "Mail relay between servers"),
    Port(53, BOTH, "dns", "DNS queries"),
    Port(67, UDP, "dhcp", "DHCP server"),
    Port(68, UDP, "dhcp", "DHCP client"),
    Port(69, UDP, "tftp", "Trivial FTP, network boot"),
    Port(80, TCP, "http", "HTTP"),
    Port(88, BOTH, "kerberos", "Kerberos authentication"),
    Port(102, TCP, "s7", "Siemens S7 PLC (ISO-TSAP)"),
    Port(110, TCP, "pop3", "POP3 mail"),
    Port(111, BOTH, "rpcbind", "ONC RPC port mapper"),
    Port(113, TCP, "ident", "Ident protocol"),
    Port(119, TCP, "nntp", "Usenet news"),
    Port(123, UDP, "ntp", "Time synchronization"),
    Port(135, TCP, "msrpc", "Microsoft RPC endpoint mapper"),
    Port(137, UDP, "netbios-ns", "NetBIOS name service"),
    Port(138, UDP, "netbios-dgm", "NetBIOS datagram service"),
    Port(139, TCP, "netbios-ssn", "NetBIOS session, SMB over NetBIOS"),
    Port(143, TCP, "imap", "IMAP mail"),
    Port(161, UDP, "snmp", "SNMP queries"),
    Port(162, UDP, "snmptrap", "SNMP traps"),
    Port(179, TCP, "bgp", "BGP routing"),
    Port(389, BOTH, "ldap", "LDAP directory"),
    Port(427, BOTH, "slp", "Service Location Protocol"),
    Port(443, BOTH, "https", "HTTPS; UDP carries HTTP/3 (QUIC)"),
    Port(445, TCP, "smb", "SMB, Windows file sharing"),
    Port(465, TCP, "smtps", "Mail submission over implicit TLS"),
    Port(500, UDP, "isakmp", "IPsec IKE"),
    Port(502, TCP, "modbus", "Modbus TCP"),
    Port(514, UDP, "syslog", "Syslog"),
    Port(515, TCP, "lpd", "Line printer daemon"),
    Port(520, UDP, "rip", "RIP routing"),
    Port(546, UDP, "dhcpv6-client", "DHCPv6 client"),
    Port(547, UDP, "dhcpv6-server", "DHCPv6 server"),
    Port(548, TCP, "afp", "Apple Filing Protocol"),
    Port(554, BOTH, "rtsp", "RTSP streaming, IP cameras"),
    Port(587, TCP, "submission", "Mail submission with STARTTLS"),
    Port(631, BOTH, "ipp", "Internet Printing Protocol, CUPS"),
    Port(636, TCP, "ldaps", "LDAP over TLS"),
    Port(853, BOTH, "domain-s", "DNS over TLS / QUIC"),
    Port(873, TCP, "rsync", "rsync daemon"),
    Port(989, TCP, "ftps-data", "FTP data over TLS"),
    Port(990, TCP, "ftps", "FTP control over TLS"),
    Port(993, TCP, "imaps", "IMAP over TLS"),
    Port(995, TCP, "pop3s", "POP3 over TLS"),
    Port(1080, TCP, "socks", "SOCKS proxy"),
    Port(1099, TCP, "rmiregistry", "Java RMI registry"),
    Port(1194, UDP, "openvpn", "OpenVPN"),
    Port(1433, TCP, "mssql", "Microsoft SQL Server"),
    Port(1434, UDP, "mssql-browser", "SQL Server Browser"),
    Port(1521, TCP, "oracle", "Oracle database listener"),
    Port(1701, UDP, "l2tp", "L2TP VPN"),
    Port(1723, TCP, "pptp", "PPTP VPN"),
    Port(1812, UDP, "radius", "RADIUS authentication"),
    Port(1813, UDP, "radius-acct", "RADIUS accounting"),
    Port(1883, TCP, "mqtt", "MQTT"),
    Port(1900, UDP, "ssdp", "SSDP / UPnP discovery"),
    Port(1935, TCP, "rtmp", "RTMP streaming"),
    Port(2049, BOTH, "nfs", "Network File System"),
    Port(2181, TCP, "zookeeper", "ZooKeeper client"),
    Port(2222, TCP, "ssh-alt", "Common alternate SSH port"),
    Port(2375, TCP, "docker", "Docker API without TLS. Never expose it"),
    Port(2376, TCP, "docker-tls", "Docker API over TLS"),
    Port(2377, TCP, "swarm", "Docker Swarm cluster management"),
    Port(2379, TCP, "etcd", "etcd client API"),
    Port(2380, TCP, "etcd-peer", "etcd peer traffic"),
    Port(3000, TCP, "dev-server", "Grafana; Node.js and Rails dev servers"),
    Port(3100, TCP, "loki", "Grafana Loki"),
    Port(3128, TCP, "squid", "Squid HTTP proxy"),
    Port(3260, TCP, "iscsi", "iSCSI storage"),
    Port(3268, TCP, "globalcatalog", "Active Directory Global Catalog"),
    Port(3306, TCP, "mysql", "MySQL and MariaDB"),
    Port(3389, BOTH, "rdp", "Remote Desktop"),
    Port(3478, BOTH, "stun", "STUN / TURN for WebRTC"),
    Port(3690, TCP, "svn", "Subversion"),
    Port(4040, TCP, "spark-ui", "Spark application UI"),
    Port(4200, TCP, "angular", "Angular dev server"),
    Port(4222, TCP, "nats", "NATS messaging"),
    Port(4317, TCP, "otlp-grpc", "OpenTelemetry OTLP over gRPC"),
    Port(4318, TCP, "otlp-http", "OpenTelemetry OTLP over HTTP"),
    Port(4369, TCP, "epmd", "Erlang port mapper, RabbitMQ"),
    Port(4500, UDP, "ipsec-nat-t", "IPsec NAT traversal"),
    Port(4789, UDP, "vxlan", "VXLAN overlay networks"),
    Port(4840, TCP, "opc-ua", "OPC UA industrial automation"),
    Port(5000, TCP, "dev-server", "Flask dev server, Docker registry, macOS AirPlay"),
    Port(5005, TCP, "jdwp", "Common Java debugger (JDWP) port"),
    Port(5037, TCP, "adb-server", "ADB server on the computer"),
    Port(5044, TCP, "beats", "Logstash Beats input"),
    Port(5060, BOTH, "sip", "SIP VoIP signaling"),
    Port(5061, TCP, "sips", "SIP over TLS"),
    Port(5173, TCP, "vite", "Vite dev server"),
    Port(5222, TCP, "xmpp", "XMPP client connections"),
    Port(5228, TCP, "fcm", "Google Play services and Firebase Cloud Messaging"),
    Port(5353, UDP, "mdns", "Multicast DNS, Bonjour"),
    Port(5432, TCP, "postgresql", "PostgreSQL"),
    Port(5554, TCP, "emulator-console", "Android emulator console"),
    Port(5555, TCP, "adb", "ADB over TCP/IP on the device"),
    Port(5601, TCP, "kibana", "Kibana"),
    Port(5672, TCP, "amqp", "AMQP, RabbitMQ"),
    Port(5683, UDP, "coap", "CoAP for IoT"),
    Port(5900, TCP, "vnc", "VNC, display :0"),
    Port(5938, TCP, "teamviewer", "TeamViewer"),
    Port(5984, TCP, "couchdb", "CouchDB"),
    Port(5985, TCP, "winrm", "Windows Remote Management over HTTP"),
    Port(5986, TCP, "winrm-https", "Windows Remote Management over HTTPS"),
    Port(6000, TCP, "x11", "X11, display :0"),
    Port(6006, TCP, "tensorboard", "TensorBoard, Storybook"),
    Port(6379, TCP, "redis", "Redis"),
    Port(6443, TCP, "kube-apiserver", "Kubernetes API server"),
    Port(6667, TCP, "irc", "IRC"),
    Port(6881, BOTH, "bittorrent", "BitTorrent"),
    Port(7000, TCP, "cassandra", "Cassandra inter-node traffic"),
    Port(7077, TCP, "spark", "Spark standalone master"),
    Port(7474, TCP, "neo4j", "Neo4j browser"),
    Port(7687, TCP, "bolt", "Neo4j Bolt protocol"),
    Port(7946, BOTH, "gossip", "Docker Swarm and Serf node discovery"),
    Port(8000, TCP, "http-alt", "Django and Python http.server"),
    Port(8009, TCP, "ajp", "Tomcat AJP connector"),
    Port(8080, TCP, "http-alt", "Alternate HTTP, Tomcat, proxies"),
    Port(8081, TCP, "metro", "React Native Metro bundler, alternate HTTP"),
    Port(8086, TCP, "influxdb", "InfluxDB HTTP API"),
    Port(8088, TCP, "yarn", "Hadoop YARN resource manager UI"),
    Port(8096, TCP, "jellyfin", "Jellyfin media server"),
    Port(8125, UDP, "statsd", "StatsD metrics"),
    Port(8161, TCP, "activemq-web", "ActiveMQ web console"),
    Port(8200, TCP, "vault", "HashiCorp Vault"),
    Port(8384, TCP, "syncthing-gui", "Syncthing web GUI"),
    Port(8443, TCP, "https-alt", "Alternate HTTPS"),
    Port(8472, UDP, "flannel", "Flannel VXLAN backend"),
    Port(8500, TCP, "consul", "Consul HTTP API"),
    Port(8761, TCP, "eureka", "Spring Cloud Eureka"),
    Port(8883, TCP, "mqtts", "MQTT over TLS"),
    Port(8888, TCP, "jupyter", "Jupyter Notebook"),
    Port(9000, TCP, "php-fpm", "PHP-FPM, SonarQube, MinIO API"),
    Port(9042, TCP, "cql", "Cassandra CQL clients"),
    Port(9090, TCP, "prometheus", "Prometheus"),
    Port(9092, TCP, "kafka", "Kafka broker"),
    Port(9093, TCP, "alertmanager", "Prometheus Alertmanager"),
    Port(9100, TCP, "node-exporter", "Prometheus node exporter, raw printing"),
    Port(9200, TCP, "elasticsearch", "Elasticsearch HTTP API"),
    Port(9222, TCP, "devtools", "Chrome remote debugging"),
    Port(9229, TCP, "node-inspect", "Node.js inspector"),
    Port(9300, TCP, "es-transport", "Elasticsearch node transport"),
    Port(9411, TCP, "zipkin", "Zipkin tracing"),
    Port(9418, TCP, "git", "Git protocol"),
    Port(9870, TCP, "hdfs-ui", "HDFS NameNode web UI"),
    Port(9993, UDP, "zerotier", "ZeroTier"),
    Port(10250, TCP, "kubelet", "Kubelet API"),
    Port(11211, BOTH, "memcached", "Memcached"),
    Port(15672, TCP, "rabbitmq-ui", "RabbitMQ management UI"),
    Port(16686, TCP, "jaeger-ui", "Jaeger tracing UI"),
    Port(19132, UDP, "minecraft-be", "Minecraft Bedrock server"),
    Port(20000, TCP, "dnp3", "DNP3 SCADA"),
    Port(22000, BOTH, "syncthing", "Syncthing file sync"),
    Port(24224, TCP, "fluentd", "Fluentd forward input"),
    Port(25565, TCP, "minecraft", "Minecraft Java server"),
    Port(27017, TCP, "mongodb", "MongoDB"),
    Port(32400, TCP, "plex", "Plex Media Server"),
    Port(41641, UDP, "tailscale", "Tailscale WireGuard traffic"),
    Port(44818, TCP, "ethernet-ip", "EtherNet/IP industrial protocol"),
    Port(47808, UDP, "bacnet", "BACnet building automation"),
    Port(50051, TCP, "grpc", "Common gRPC example port"),
    Port(51820, UDP, "wireguard", "WireGuard default"),
    Port(61616, TCP, "activemq", "ActiveMQ OpenWire")
)

internal fun filterPorts(query: String): List<Port> {
    val q = query.trim()
    if (q.isEmpty()) return PORTS
    return PORTS.filter {
        it.number.toString().startsWith(q) || it.service.contains(q, true) || it.note.contains(q, true) || it.protocol.contains(q, true)
    }
}

private val LISTED_PORTS = PORTS.mapTo(HashSet()) { it.number }

/** A port in the dynamic range 49152-65535 that is not in the list above. */
internal fun randomFreePort(random: Random = Random.Default): Int {
    while (true) {
        val port = random.nextInt(49152, 65536)
        if (port !in LISTED_PORTS) return port
    }
}

private fun portRange(port: Int) = when {
    port < 1024 -> "System ports (0-1023)"
    port < 49152 -> "Registered ports (1024-49151)"
    else -> "Dynamic ports (49152-65535)"
}

class PortReferenceTool : Tool {
    override val descriptor = ToolDescriptor(
        "port-reference",
        "Well-Known Ports",
        "Look up common TCP and UDP port numbers",
        Icons.Default.SettingsEthernet,
        ToolCategory.NETWORK_SYSTEM,
        listOf("port", "ports", "tcp", "udp", "service", "firewall", "random port")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var query by rememberSaveable { mutableStateOf("") }
        var randomPort by rememberSaveable { mutableStateOf<Int?>(null) }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        val copyPort: (Int) -> Unit = {
            clipboardManager.setText(AnnotatedString(it.toString()))
            confirmCopied(context, "Port")
        }
        val groups = remember(query) { filterPorts(query).groupBy { portRange(it.number) }.toList() }

        ToolScaffold(descriptor.title, modifier = modifier, scrollable = false) {
            SearchField(query, { query = it }, "Search by port, service or protocol")
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { randomPort = randomFreePort() }) { Text("Random free port") }
                randomPort?.let { port ->
                    Text(
                        port.toString(),
                        style = CodeTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClickLabel = "Copy port") { copyPort(port) }
                            .padding(8.dp)
                    )
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (groups.isEmpty()) {
                    item { EmptyHint("No port matches \"${query.trim()}\". Try a number like 5432 or a name like redis.") }
                }
                groups.forEach { (range, ports) ->
                    item(key = range) { SectionHeader(range, true, onToggle = null) }
                    items(ports, key = { it.number }) { port ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Copy port") { copyPort(port.number) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                port.number.toString(),
                                style = CodeTextStyle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(64.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text("${port.service}  ·  ${port.protocol}", style = MaterialTheme.typography.bodyLarge)
                                Text(port.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
