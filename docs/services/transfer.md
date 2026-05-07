# Transfer Family

**Protocol:** JSON 1.1  
**Endpoint:** `http://localhost:4566/`  
**X-Amz-Target prefix:** `TransferService.`

## Supported Actions

### Servers

| Action | Description |
|---|---|
| `CreateServer` | Create an SFTP/FTP/FTPS server |
| `DescribeServer` | Get server details and state |
| `UpdateServer` | Update server configuration |
| `DeleteServer` | Delete a server |
| `ListServers` | List all servers |
| `StartServer` | Start a stopped server (sets state to `ONLINE`) |
| `StopServer` | Stop a running server (sets state to `OFFLINE`) |

### Users

| Action | Description |
|---|---|
| `CreateUser` | Create a user on a server |
| `DescribeUser` | Get user details and SSH keys |
| `UpdateUser` | Update user properties (home directory, role, policy) |
| `DeleteUser` | Delete a user and all associated SSH keys |
| `ListUsers` | List all users for a server |

### SSH Public Keys

| Action | Description |
|---|---|
| `ImportSshPublicKey` | Import an SSH public key for a user |
| `DeleteSshPublicKey` | Remove an SSH public key from a user |

### Tagging

| Action | Description |
|---|---|
| `TagResource` | Add tags to a server or user |
| `UntagResource` | Remove tags from a resource |
| `ListTagsForResource` | List tags on a resource |

## Configuration

```yaml
floci:
  services:
    transfer:
      enabled: true
```

| Environment variable | Default | Description |
|---|---|---|
| `FLOCI_SERVICES_TRANSFER_ENABLED` | `true` | Enable or disable Transfer Family |

## ARN Format

| Resource | ARN |
|---|---|
| Server | `arn:aws:transfer:{region}:{account}:server/{serverId}` |
| User | `arn:aws:transfer:{region}:{account}:user/{serverId}/{userName}` |

## Example Usage

```bash
# Create a server
aws --endpoint-url http://localhost:4566 transfer create-server \
  --protocols SFTP

# Create a user
aws --endpoint-url http://localhost:4566 transfer create-user \
  --server-id s-abc123 \
  --user-name alice \
  --role arn:aws:iam::000000000000:role/transfer-role \
  --home-directory /alice

# Import an SSH public key
aws --endpoint-url http://localhost:4566 transfer import-ssh-public-key \
  --server-id s-abc123 \
  --user-name alice \
  --ssh-public-key-body "ssh-rsa AAAA..."

# List servers
aws --endpoint-url http://localhost:4566 transfer list-servers

# Start / stop a server
aws --endpoint-url http://localhost:4566 transfer start-server --server-id s-abc123
aws --endpoint-url http://localhost:4566 transfer stop-server  --server-id s-abc123
```

## Notes

- **Phase 1** covers the management-plane API only. Data-plane SFTP connectivity (actual file transfer) is not emulated.
- Server `EndpointType` defaults to `PUBLIC`. The `State` field transitions between `ONLINE` and `OFFLINE` via `StartServer` / `StopServer`.
- SSH key bodies are stored and returned as-is; no cryptographic validation is performed.
