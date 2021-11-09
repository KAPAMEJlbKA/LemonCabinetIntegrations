# SimpleCabinetBukkitPlugin
Плагин Bukkit для интеграции с SimpleCabinet
#### Возможности
- Интеграция с Vault(экономика)
- Передача монет другому игроку
- Выдача предметов с поддержкой NBT
#### Выдача предметов с NBT
- **Item-NBT-API** современный плагин NBT, работающий на версиях 1.7.10 - 1.17.1
- **PowerNBT** устаревший плагин для старых версий
- **Bukkit-based** выдача стандартных тегов Bukkit(displayName/Lore) без плагинов
#### Установка
- Скопируйте плагин в папку plugins вашего сервера и запустите его для появления примера конфигурации 
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
- **simplecabinet.commands.economy.balance** - право использовать `/economy balance`
- **simplecabinet.commands.shop** - право использовать команду `/shop`
- **simplecabinet.commands.shop.all** - право использовать `/shop all`
