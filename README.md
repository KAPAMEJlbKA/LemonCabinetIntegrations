# SimpleCabinetBukkitPlugin
Плагин Bukkit для интеграции с SimpleCabinet
#### Установка
- Скопируйте плагин в папку plugins вашего сервера
- Настройте конфигурацию:
```json
{
  "url": "http://АДРЕС ЛК/",
  "token": "ТОКЕН ДОСТУПА",
  "economy": {
    "enabled": true,
    "vault": true,
    "defaultCurrency": "ECO",
    "transactionComment": "Economy",
    "bankEmulator": false
  },
  "testOnStartup": true
}
```
#### Привилегии
- **simplecabinet.commands.economy** - право использовать команду `/economy`
- **simplecabinet.commands.economy.transfer** - право использовать `/economy transfer`
- **simplecabinet.commands.shop** - право использовать команду `/shop`
- **simplecabinet.commands.shop.all** - право использовать `/shop all`
