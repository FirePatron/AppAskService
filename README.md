# AppAskService
В onCreate инициализируются все сервисы, запрашивается uuid у OneSignal через AsyncGetTags. Полученые данные проверяются и срабатывает один из трёх сценариев
1. Первый вход
2. Переход к рулетке
3. Переход на заглушку
