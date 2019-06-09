# **Modification** of Modification of the launcher sashok724's v3 from Gravit **with VK Auhtorization from JoshOOOWAH**
### Установка:
* Создайте приложение типа "Standalone" по [ссылке](https://vk.com/editapp?act=create)
* Из вкладки "Настройки" скопируйте "ID приложения" и "Защищённый ключ"
* В файле `LaunchServer.conf` заполните элемент 'OAuth':
    ID - ID приложения 
    Secret - Защищённый ключ 
    BackURL - ссылка на Веб-скрипт связи с LaunchServer 
* Так же в файле конфигурации должен быть AuthProvider c типом mysql и названием *MySQLProvider* 
* Внутри AuthProvider надо добавить строчку `"oAuthQuery": "SELECT login FROM mcr_users WHERE OAuthID=? LIMIT 1"`
*не забудьте создать колонку `OAuthID` в базе данных*
* Для работы Веб-скрипта необходимо запустить web-сервер, я использую *nginx*
* В самом Веб-скрипте необходимо в строчке `var wsUri = "ws://localhost:9274/api";
` изменить `localhost` на ip LaunchServer
* [Веб-скрипт](https://github.com/JoshOOOOWAH/Launcher/blob/master/compat/OAuth.html)
* [Пример конфигурации](https://github.com/JoshOOOOWAH/Launcher/blob/master/compat/LaunchServer.conf)