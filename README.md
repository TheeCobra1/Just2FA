# Just2FA - Advanced Two-Factor Authentication for Minecraft

[![Spigot Version](https://img.shields.io/badge/Spigot-1.13--1.20+-orange.svg)](https://www.spigotmc.org/)
[![Java Version](https://img.shields.io/badge/Java-8+-blue.svg)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![WorldGuard Compatible](https://img.shields.io/badge/WorldGuard-Compatible-purple.svg)](https://dev.bukkit.org/projects/worldguard)

Just2FA is a comprehensive two-factor authentication (2FA) plugin for Minecraft servers that provides enterprise-grade security through Google Authenticator integration. Featuring unique map-based QR codes, granular player restrictions, and seamless WorldGuard integration, Just2FA ensures your server and player accounts remain secure.

## 🌟 Key Features

### 🔐 **Advanced Authentication System**
- **Google Authenticator Integration** - Industry-standard TOTP authentication
- **AES-256 Encryption** - Military-grade encryption for stored secrets
- **Session Management** - Configurable authentication sessions with timeout
- **Failed Attempt Protection** - Automatic lockout after multiple failed attempts
- **IP Tracking** - Monitor and log player login locations

### 🗺️ **Unique Map-Based QR Code System**
- **In-Game QR Display** - Generate QR codes directly on Minecraft maps
- **Secure Map Handling** - Automatic destruction after setup, prevents sharing
- **Alternative Display Options** - ASCII QR codes in chat as backup
- **Dynamic Generation** - Real-time QR code creation with proper scaling

### 🚫 **Comprehensive Player Restrictions**
When not authenticated, players are restricted from:
- Movement (configurable spawn area containment)
- Block breaking and placing
- Inventory interactions
- Item dropping and pickup
- Combat and damage
- Chat and commands (except 2FA commands)
- Teleportation and portal usage
- Vehicle interactions
- Consuming food/potions
- And 10+ more configurable actions

### 🌍 **WorldGuard Integration**
- **Custom Flags** - Region-specific 2FA requirements
  - `require-2fa` - Force 2FA in specific regions
  - `bypass-2fa` - Allow bypassing in safe zones
  - `2fa-allow-movement` - Control movement per region
- **Dynamic Policies** - Different rules for different areas
- **Backward Compatibility** - Works with multiple WorldGuard versions

### 💾 **Robust Data Management**
- **Async Operations** - Non-blocking save/load operations
- **Auto-Save System** - Automatic data persistence every 5 minutes
- **Encrypted Storage** - Secure player data storage
- **In-Memory Caching** - Optimized performance with smart caching

## 📦 Installation

1. **Download** the latest Just2FA.jar from [Releases](../../releases)
2. **Place** the JAR file in your server's `plugins` folder
3. **Restart** your server
4. **Configure** the plugin using `/2fa admin reload` after editing config.yml

### Dependencies
- **Required**: Spigot/Paper 1.13+, Java 8+
- **Optional**: WorldGuard 7.0+, WorldEdit 7.0+

## 🎮 Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/2fa setup` | Begin 2FA setup process | `just2fa.use` |
| `/2fa verify <code>` | Verify your authentication code | `just2fa.use` |
| `/2fa remove` | Remove 2FA from your account | `just2fa.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/2fa admin remove <player>` | Remove 2FA from a player | `just2fa.admin.remove` |
| `/2fa admin reset <player>` | Reset player's 2FA data | `just2fa.admin.remove` |
| `/2fa admin reload` | Reload configuration | `just2fa.admin.reload` |

**Command Aliases**: `/auth`, `/authenticator`

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `just2fa.use` | Access to basic 2FA commands | Everyone |
| `just2fa.bypass` | Bypass 2FA requirement | False |
| `just2fa.admin` | Access to all admin commands | OP |
| `just2fa.admin.remove` | Remove 2FA from other players | OP |
| `just2fa.admin.reload` | Reload plugin configuration | OP |

## ⚙️ Configuration

Just2FA offers extensive configuration options. Key settings include:

### Authentication Settings
```yaml
authentication:
  session-timeout: 3600  # Seconds before re-authentication required
  code-length: 6         # Length of authentication codes
  window-size: 3         # Time window tolerance (±90 seconds)
  secret-key-length: 16  # Length of generated secrets
```

### Security Settings
```yaml
security:
  max-attempts: 5           # Max failed attempts before lockout
  lockout-time: 300         # Lockout duration in seconds
  require-2fa: false        # Force 2FA for all players
  kick-timeout: 300         # Kick unauthenticated players after X seconds
```

### Spawn Protection
```yaml
spawn-protection:
  enabled: true
  radius: 10              # Restriction radius from spawn
  freeze-at-spawn: true   # Completely freeze players
  teleport-to-spawn: true # Teleport to spawn on join
```

### Restricted Actions
```yaml
restricted-actions:
  block-break: true
  block-place: true
  player-interact: true
  inventory-click: true
  item-drop: true
  item-pickup: true
  # ... and 13+ more configurable actions
```

## 🚀 Quick Start Guide

### For Players
1. Join the server
2. Run `/2fa setup` to begin setup
3. Receive a map with QR code (or ASCII in chat)
4. Scan with Google Authenticator app
5. Enter the 6-digit code with `/2fa verify <code>`
6. You're protected! Re-authenticate when required

### For Administrators
1. Install the plugin
2. Configure settings in `config.yml`
3. Set up WorldGuard regions if desired
4. Use `/2fa admin` commands for management
5. Monitor player authentications through logs

## 🛡️ Security Features

- **AES-256 Encryption** - All secrets encrypted at rest
- **TOTP Algorithm** - Time-based codes expire every 30 seconds
- **Anti-Brute Force** - Automatic lockout after failed attempts
- **Session Management** - Configurable re-authentication requirements
- **Map Security** - QR maps can't be shared or duplicated
- **IP Logging** - Track login locations for audit trails

## 🔧 WorldGuard Integration

When WorldGuard is installed, Just2FA adds custom flags:

```yaml
# Example region setup
/rg flag spawn require-2fa true       # Force 2FA in spawn
/rg flag safezone bypass-2fa true     # No 2FA in safe zones
/rg flag vault 2fa-allow-movement false # No movement until authenticated
```

## 📊 Performance

- **Async Operations** - All I/O operations are asynchronous
- **Smart Caching** - Reduces database/file access
- **Efficient Events** - Optimized event handling
- **Minimal Overhead** - Lightweight with minimal server impact

## 🐛 Troubleshooting

### Common Issues

**QR Code Not Displaying**
- Ensure you have an empty inventory slot
- Check if map-based QR is enabled in config
- Try ASCII display as alternative

**Authentication Failing**
- Verify device time is synchronized
- Check time window settings
- Ensure Google Authenticator is properly configured

**WorldGuard Flags Not Working**
- Verify WorldGuard is properly installed
- Check flag permissions
- Reload both plugins

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**TheeCobra1** - [GitHub Profile](https://github.com/TheeCobra1)

## 🙏 Acknowledgments

- Google Authenticator Library for TOTP implementation
- ZXing Library for QR code generation
- Spigot/Paper community for API support
- WorldGuard team for integration capabilities

## 📈 Version History

- **1.0.1** - Current version
  - Bug fixes and performance improvements
  - Enhanced security features
  
- **1.0.0** - Initial release with full feature set
  - Google Authenticator integration
  - Map-based QR codes
  - WorldGuard support
  - Comprehensive player restrictions

---

<div align="center">
  
**[Report Issues](../../issues) | [Request Features](../../issues) | [View Source](../../tree/master)**

Made with ❤️ for the Minecraft community

</div>